package niohandler

import network.ConnectionCtx
import storage.StorageService

class StringCommandHandler(
    private val storageService: StorageService,
) : CommandHandler {
    private val cmds = setOf("SET", "GET")

    override fun isHandle(cmd: String): Boolean {
        return cmds.contains(cmd)
    }

    override fun handle(connectionCtx: ConnectionCtx, request: List<String>) {
        val cmd = request[0]
        val args = request.drop(1)
        when (cmd) {
            "SET" -> set(connectionCtx, args)
            "GET" -> get(connectionCtx, args)
        }
    }

    private fun set(connectionCtx: ConnectionCtx, args: List<String>) {
        val key = args[0]
        val value = args[1]
        if (args.size == 4 && args[2].uppercase() == "PX") {
            storageService.setString(key, value, args[3].toLong() + System.currentTimeMillis())
        } else {
            storageService.setString(key, value, null)
        }
        connectionCtx.writeSimpleString("OK")
    }

    private fun get(connectionCtx: ConnectionCtx, args: List<String>) {
        val key = args[0]
        val value = storageService.getString(key)
        if(value != null){
            connectionCtx.writeBulkString(value)
            return
        }
        connectionCtx.writeNil()
    }
}
