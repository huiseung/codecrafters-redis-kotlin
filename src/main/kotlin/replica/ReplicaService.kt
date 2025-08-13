package replica

import config.RedisConfig
import network.ConnectionCtx
import network.ConnectionType
import protocol.RespWriter
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class ReplicaService(
    private val config: RedisConfig,
    private val respWriter: RespWriter,
    private val selector: Selector,
) {
    fun run() {
        if (config.get("role") != "slave") {
            return
        }
        val masterChannel = connectToMaster()
        masterChannel.configureBlocking(false)
        val key = masterChannel.register(selector, SelectionKey.OP_READ)
        key.attach(ConnectionCtx(key, ConnectionType.FOR_MASTER))
    }

    private fun connectToMaster(): SocketChannel {
        if (config.get("role") == "master") {
            throw IllegalArgumentException()
        }
        val socketAddress = InetSocketAddress(config.get("master_host"), config.get("master_port")!!.toInt())
        val masterChannel = SocketChannel.open(socketAddress)
        masterChannel.configureBlocking(true)
        handshake(masterChannel)
        return masterChannel
    }

    private fun handshake(masterChannel: SocketChannel) {
        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("PING")))
        var line = readSimpleLine(masterChannel) ?: return
        if (line != "+PONG") return

        masterChannel.write(
            respWriter.writeArrayOfBulkString(
                listOf(
                    "REPLCONF",
                    "listening-port",
                    "${config.get("port")}"
                )
            )
        )
        line = readSimpleLine(masterChannel) ?: throw IllegalArgumentException()
        if (line != "+OK") return

        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("REPLCONF", "capa", "psync2")))
        line = readSimpleLine(masterChannel) ?: throw IllegalArgumentException()
        if (line != "+OK") return

        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("PSYNC", "?", "-1")))
        line = readSimpleLine(masterChannel) ?: throw IllegalArgumentException()
        if (!line.startsWith("+FULLRESYNC")) return
    }

    private fun readSimpleLine(ch: SocketChannel): String? {
        val buf = ByteBuffer.allocate(8 * 1024)
        val out = StringBuilder()
        while (true) {
            buf.clear()
            val n = ch.read(buf)
            if (n <= 0) return null
            buf.flip()
            val s = StandardCharsets.UTF_8.decode(buf).toString()
            out.append(s)
            val idx = out.indexOf("\r\n")
            if (idx >= 0) {
                return out.substring(0, idx)
            }
        }
    }
}
