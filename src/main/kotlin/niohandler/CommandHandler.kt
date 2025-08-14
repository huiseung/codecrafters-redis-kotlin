package niohandler

import network.ConnectionCtx
import protocol.Request

interface CommandHandler {
    fun isHandle(cmd: String): Boolean
    fun handle(connectionCtx: ConnectionCtx, request: Request)
}
