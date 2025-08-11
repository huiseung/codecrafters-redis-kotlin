package handler.command

import config.RedisConfig
import network.Connection
import protocol.CommandResultWriter

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
            commandResultWriter.writeBulkString(connection, "role:${config.get("role")}")
        }
    }
}
