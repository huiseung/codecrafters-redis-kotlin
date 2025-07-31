package handler.command

import Connection
import protocol.CommandResultWriter

class BasicCommandHandler(
    private val commandResultWriter: CommandResultWriter = CommandResultWriter()
) : CommandHandler {
    private val handleCommands = setOf("ECHO", "PING")

    override fun isHandle(cmd: String): Boolean {
        return handleCommands.contains(cmd)
    }

    override fun handle(connection: Connection) {
        when (connection.cmd) {
            "PING" -> ping(connection)
            "ECHO" -> echo(connection)
        }
    }

    private fun ping(connection: Connection) {
        commandResultWriter.writeSimpleString(connection, "PONG")
    }

    private fun echo(connection: Connection) {
        val value = connection.args[0]
        commandResultWriter.writeBulkString(connection, value)
    }
}
