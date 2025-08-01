package handler.command

import Connection

interface CommandHandler {
    fun isHandle(cmd: String): Boolean
    suspend fun handle(connection: Connection)
}
