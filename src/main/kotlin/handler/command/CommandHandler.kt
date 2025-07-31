package handler.command

import Connection

interface CommandHandler {
    fun isHandle(cmd: String): Boolean
    fun handle(connection: Connection)
}
