package niohandler

import config.RedisConfig
import network.ConnectionCtx
import network.ConnectionType
import protocol.Request
import protocol.RespWriter
import replica.PendingWait
import replica.ReplicaService
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

class ReplicaCommandHandler(
    private val config: RedisConfig,
    private val respWriter: RespWriter,
    private val replicaService: ReplicaService,

    ) : CommandHandler {
    private val cmds = setOf("INFO", "REPLCONF", "PSYNC", "WAIT")
    private val replid = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"

    override fun isHandle(cmd: String): Boolean {
        return cmds.contains(cmd)
    }

    override fun handle(connection: ConnectionCtx, request: Request) {
        val cmd = request.request[0]
        when (cmd) {
            "INFO" -> info(connection, request)
            "REPLCONF" -> replconf(connection, request)
            "PSYNC" -> psync(connection, request)
            "WAIT" -> wait(connection, request)
        }
    }

    private fun info(connection: ConnectionCtx, request: Request) {
        val args = request.request.drop(1)
        val key = args[0]
        if (key == "replication") {
            connection.writeBuffer(respWriter.writeBulkString("role:${config.get("role")}\r\nmaster_replid:$replid\r\nmaster_repl_offset:0"))
        }
    }

    private fun replconf(connection: ConnectionCtx, request: Request) {
        val args = request.request.drop(1)
        if (args.isNotEmpty()) {
            when (args[0].uppercase()) {
                "GETACK" -> {
                    connection.writeBuffer(
                        respWriter.writeArrayOfBulkString(
                            listOf(
                                "REPLCONF",
                                "ACK",
                                "${connection.offset}"
                            )
                        )
                    )
                    if (connection.connectionType == ConnectionType.FOR_MASTER) {
                        connection.plusOffset(request.bytes)
                    }
                }

                "ACK" -> {
                    val ack = args[1].toLong()
                    connection.offset = ack
                }

                "LISTENING-PORT", "CAPA" -> {
                    connection.writeBuffer(respWriter.writeSimpleString("OK"))
                }
            }
        }

    }

    private fun psync(connection: ConnectionCtx, request: Request) {
        connection.writeBuffer(respWriter.writeSimpleString("FULLRESYNC $replid 0"))
        val dir = config.get("dir")
        val dbfilename = config.get("dbfilename")
        val file = File(dir, dbfilename)
        if (!file.exists()) return
        val len = file.length()
        run {
            val header = ByteBuffer.wrap(("\$$len\r\n").toByteArray(UTF_8))
            connection.writeBuffer(header)
        }
        FileInputStream(file).channel.use { ch ->
            val buffer = ByteBuffer.allocate(8192) // 8192 = BufferedInputStream capa default 값
            while (ch.read(buffer) > 0) {
                buffer.flip()
                val copy = ByteBuffer.allocate(buffer.remaining())
                copy.put(buffer)
                copy.flip()
                connection.writeBuffer(copy)
                buffer.clear()
            }
        }
        connection.connectionType = ConnectionType.FOR_REPLICA
        replicaService.registerReplica(connection)
    }

    private fun wait(connection: ConnectionCtx, request: Request) {
        val args = request.request.drop(1)
        val numReplica = args[0].toInt()
        val timeoutMs = args[1].toLong()

        if (replicaService.replicas.isEmpty()) {
            connection.writeBuffer(respWriter.writeInteger(0))
            return
        }
        connection.disableReadInterest()
        val target = replicaService.masterOffset

        val payload = respWriter.writeArrayOfBulkString(listOf("REPLCONF", "GETACK", "*"))
        for (repl in replicaService.replicas) {
            val dup = payload.duplicate()
            dup.position(0)
            dup.limit(payload.limit())
            repl.writeBuffer(dup)
        }
        val deadline = System.currentTimeMillis() + timeoutMs
        replicaService.offer(PendingWait(connection, target, numReplica, deadline))
    }
}
