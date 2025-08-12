package niohandler

import config.RedisConfig
import network.ConnectionCtx
import storage.StorageService

class BasicCommandHandler(
    private val config: RedisConfig,
    private val storageService: StorageService,
) : CommandHandler {
    private val cmds = setOf("PING", "ECHO", "CONFIG", "KEYS")
    override fun isHandle(cmd: String): Boolean {
        return cmds.contains(cmd)
    }

    override fun handle(connectionCtx: ConnectionCtx, request: List<String>) {
        val cmd = request[0]
        val args = request.drop(1)
        when (cmd) {
            "PING" -> ping(connectionCtx)
            "ECHO" -> echo(connectionCtx, args)
            "CONFIG" -> config(connectionCtx, args)
            "KEYS" -> keys(connectionCtx, args)
        }
    }

    private fun ping(connectionCtx: ConnectionCtx) {
        connectionCtx.writeSimpleString("PONG")
    }

    private fun echo(connectionCtx: ConnectionCtx, args: List<String>) {
        connectionCtx.writeBulkString(args[0])
    }

    private fun config(connectionCtx: ConnectionCtx, args: List<String>) {
        val value = args[0]
        if (value.uppercase() == "GET") {
            val param = args[1]
            val value = config.get(param)
            if (value != null) {
                val ret = mutableListOf<String>()
                ret.add(param)
                ret.add(value)
                connectionCtx.writeArrayOfBulkString(ret)
            }
        }
    }

    private fun keys(connectionCtx: ConnectionCtx, args: List<String>) {
        val value = args[0]
        val ret = storageService.keys(value)
        connectionCtx.writeArrayOfBulkString(ret)
    }
}
