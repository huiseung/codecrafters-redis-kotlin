package handler.command

import Connection
import StorageService
import protocol.CommandResultWriter


class ListCommandHandler(
    val storageService: StorageService,
    private val commandResultWriter: CommandResultWriter,
) : CommandHandler {
    private val handleCmds = setOf("RPUSH", "LRANGE", "LPUSH", "LLEN", "LPOP", "BLPOP")

    override fun isHandle(cmd: String): Boolean {
        return handleCmds.contains(cmd)
    }

    override suspend fun handle(connection: Connection) {
        when (connection.cmd) {
            "RPUSH" -> rpush(connection)
            "LPUSH" -> lpush(connection)
            "LRANGE" -> lrange(connection)
            "LLEN" -> llen(connection)
            "LPOP" -> lpop(connection)
            "BLPOP" -> blpop(connection)
        }
    }


    private suspend fun rpush(connection: Connection) {
        val key = connection.args[0]
        val values = connection.args.drop(1)
        val ret = storageService.rpush(key, values)
        commandResultWriter.writeInteger(connection, ret)
    }

    private suspend fun lpush(connection: Connection) {
        val key = connection.args[0]
        val values = connection.args.drop(1)
        commandResultWriter.writeInteger(connection, storageService.lpush(key, values))
    }

    private suspend fun lrange(connection: Connection) {
        val key = connection.args[0]
        var start = connection.args[1].toInt()
        var end = connection.args[2].toInt()
        commandResultWriter.writeArrayOfBulkString(connection, storageService.lrange(key, start, end))
    }

    private suspend fun llen(connection: Connection) {
        val key = connection.args[0]
        commandResultWriter.writeInteger(connection, storageService.llen(key))
    }

    private suspend fun lpop(connection: Connection) {
        val key = connection.args[0]
        val count = if (connection.argCount > 1) connection.args[1].toInt() else 1
        val result = storageService.lpop(key, count)
        when {
            result == null -> {
                if (count == 1) commandResultWriter.writeNIL(connection)
                else commandResultWriter.writeArrayOfBulkString(connection, emptyList())
            }

            count == 1 -> commandResultWriter.writeBulkString(connection, result.first())
            else -> commandResultWriter.writeArrayOfBulkString(connection, result)
        }
    }

    private suspend fun blpop(connection: Connection) {
        val key = connection.args[0]
        val timeout = connection.args[1].toDouble() * 1000
        val ret = storageService.blpop(key, timeout.toLong())
        if (ret == null) {
            commandResultWriter.writeNIL(connection)
            return
        }
        commandResultWriter.writeArrayOfBulkString(connection, ret.toList())
    }
}
