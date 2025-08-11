package handler.command

import config.RedisConfig
import network.Connection
import protocol.CommandResultWriter
import java.lang.StringBuilder

class ReplicaCommandHandler(
    private val config: RedisConfig,
    private val commandResultWriter: CommandResultWriter,
) : CommandHandler {
    private val handleCommands = setOf("INFO")
    override fun isHandle(cmd: String): Boolean {
        return handleCommands.contains(cmd)
    }

    override fun handle(connection: Connection) {
        when (connection.cmd) {
            "INFO" -> info(connection)
        }
    }

    private fun info(connection: Connection) {
        val key = connection.args[0]
        if (key == "replication") {
            val sb = StringBuilder()
            val replicationId = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"
            sb
                .append("master_replid:${replicationId}").append("\r\n")
                .append("role:${config.get("role")}").append("\r\n")
                .append("master_repl_offset:0")
            commandResultWriter.writeBulkString(connection, sb.toString())
        }
    }
}
