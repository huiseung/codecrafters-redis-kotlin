package handler.command

import Connection
import RedisConfig
import protocol.CommandResultWriter

class BasicCommandHandler(
    private val commandResultWriter: CommandResultWriter,
    private val redisConfig: RedisConfig,
) : CommandHandler {
    private val handleCommands = setOf("ECHO", "PING", "CONFIG")

    override fun isHandle(cmd: String): Boolean {
        return handleCommands.contains(cmd)
    }

    override suspend fun handle(connection: Connection) {
        when (connection.cmd) {
            "PING" -> ping(connection)
            "ECHO" -> echo(connection)
            "CONFIG" -> config(connection)
        }
    }

    private fun ping(connection: Connection) {
        commandResultWriter.writeSimpleString(connection, "PONG")
    }

    private fun echo(connection: Connection) {
        val value = connection.args[0]
        commandResultWriter.writeBulkString(connection, value)
    }

    private fun config(connection: Connection) {
        val value = connection.args[0]
        if (value.uppercase() == "GET") {
            val param = connection.args[1]
            val value = redisConfig.get(param)
            if (value != null) {
                val ret = mutableListOf<String>()
                ret.add(param)
                ret.add(value)
                commandResultWriter.writeArrayOfBulkString(connection, ret)
            }
        }
    }
}
