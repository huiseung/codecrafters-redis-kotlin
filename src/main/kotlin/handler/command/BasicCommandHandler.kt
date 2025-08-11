package handler.command


import config.RedisConfig
import network.Connection
import storage.StorageService
import protocol.CommandResultWriter

class BasicCommandHandler(
    private val commandResultWriter: CommandResultWriter,
    private val redisConfig: RedisConfig,
    private val storageService: StorageService,
) : CommandHandler {
    private val handleCommands = setOf("ECHO", "PING", "CONFIG", "KEYS")

    override fun isHandle(cmd: String): Boolean {
        return handleCommands.contains(cmd)
    }

    override fun handle(connection: Connection) {
        when (connection.cmd) {
            "PING" -> ping(connection)
            "ECHO" -> echo(connection)
            "CONFIG" -> config(connection)
            "KEYS" -> keys(connection)
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

    private fun keys(connection: Connection) {
        val value = connection.args[0]
        val ret = storageService.keys(value)
        commandResultWriter.writeArrayOfBulkString(connection, ret)
    }
}
