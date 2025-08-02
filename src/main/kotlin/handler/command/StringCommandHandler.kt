package handler.command

import Connection
import StorageService
import protocol.CommandResultWriter

class StringCommandHandler(
    private val storageService: StorageService,
    private val commandResultWriter: CommandResultWriter
) : CommandHandler {
    private val handleCommands = setOf("SET", "GET")

    override fun isHandle(cmd: String): Boolean {
        return handleCommands.contains(cmd)
    }

    override fun handle(connection: Connection) {
        when (connection.cmd) {
            "SET" -> set(connection)
            "GET" -> get(connection)
        }
    }

    private fun set(connection: Connection) {
        val key = connection.args[0]
        val value = connection.args[1]
        if (connection.argCount == 4 && connection.args[2].uppercase() == "PX") {
            storageService.setString(key, value, connection.args[3].toLong() + System.currentTimeMillis())
        } else {
            storageService.setString(key, value, null)
        }
        commandResultWriter.writeSimpleString(connection, "OK")
    }

    private fun get(connection: Connection) {
        val key = connection.args[0]
        val value = storageService.getString(key)
        if (value != null) {
            commandResultWriter.writeBulkString(connection, value)
            return
        }
        commandResultWriter.writeNIL(connection)
    }
}
