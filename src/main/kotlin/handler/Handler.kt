package handler

import Connection
import ConnectionType
import StorageService
import handler.command.BasicCommandHandler
import handler.command.CommandHandler
import handler.command.ListCommandHandler
import handler.command.StringCommandHandler
import protocol.CommandReader
import protocol.CommandResultWriter
import java.lang.IllegalArgumentException
import java.net.Socket

class Handler(
    private val clientSocket: Socket,
    private val commandHandlers: List<CommandHandler>,
    private val commandReader: CommandReader,
    private val commandResultWriter: CommandResultWriter,
) {
    private val connection: Connection by lazy {
        Connection(clientSocket, ConnectionType.TO_NORMAL)
    }

    suspend fun handle() {
        connection.clientSocket.use {
            while (true) {
                when (connection.connectionType) {
                    ConnectionType.TO_NORMAL, ConnectionType.TO_MASTER -> {
                        try {
                            commandReader.read(connection)
                            if (connection.cmd.isBlank()) {
                                break
                            }
                            var commandHandler = commandHandlers.firstOrNull() { it.isHandle(connection.cmd) }
                                ?: throw IllegalArgumentException("can't handle command: ${connection.cmd} .")
                            commandHandler.handle(connection)
                        } catch (e: Exception) {
                            println("ERROR: ${e.message}")
                            commandResultWriter.writeError(connection, e.message)
                        }
                    }

                    ConnectionType.FOR_FULLSYNC -> {

                    }
                }
            }
        }
    }
}
