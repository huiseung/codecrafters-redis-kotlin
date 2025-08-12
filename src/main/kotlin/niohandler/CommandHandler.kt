package niohandler

import network.ConnectionCtx

interface CommandHandler {
    fun isHandle(cmd: String): Boolean
    fun handle(connectionCtx: ConnectionCtx, request: List<String>)
}
