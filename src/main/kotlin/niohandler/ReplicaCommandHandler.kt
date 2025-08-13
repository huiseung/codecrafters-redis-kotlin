package niohandler

import config.RedisConfig
import network.ConnectionCtx
import protocol.RespWriter

class ReplicaCommandHandler(
    private val config: RedisConfig,
    private val respWriter: RespWriter,
) : CommandHandler {
    private val cmds = setOf("INFO", "REPLCONF")

    override fun isHandle(cmd: String): Boolean {
        return cmds.contains(cmd)
    }

    override fun handle(connection: ConnectionCtx, request: List<String>) {
        val cmd = request[0]
        val args = request.drop(1)
        when (cmd) {
            "INFO" -> info(connection, args)
            "REPLCONF" -> info(connection, args)
        }
    }

    private fun info(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        if (key == "replication") {
            val replid = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"
            connection.writeBuffer(respWriter.writeBulkString("role:${config.get("role")}\r\nmaster_replid:$replid\r\nmaster_repl_offset:0"))
        }
    }

    private fun replconf(connection: ConnectionCtx, args: List<String>) {
        connection.writeBuffer(respWriter.writeSimpleString("OK"))
    }
}
