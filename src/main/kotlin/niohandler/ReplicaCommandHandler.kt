package niohandler

import config.RedisConfig
import network.ConnectionCtx

class ReplicaCommandHandler(
    private val config: RedisConfig,
) : CommandHandler {
    private val cmds = setOf("INFO")

    override fun isHandle(cmd: String): Boolean {
        return cmds.contains(cmd)
    }

    override fun handle(connection: ConnectionCtx, request: List<String>) {
        val cmd = request[0]
        val args = request.drop(1)
        when (cmd) {
            "INFO" -> info(connection, args)
        }
    }

    private fun info(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        if (key == "replication") {
            connection.writeBulkString("role:${config.get("role")}")
        }
    }
}
