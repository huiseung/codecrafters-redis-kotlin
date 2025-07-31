package handler.command

import Connection
import StorageService
import protocol.CommandResultWriter
import kotlin.math.min

class ListCommandHandler(
    val storageService: StorageService,
    private val commandResultWriter: CommandResultWriter = CommandResultWriter(),
) : CommandHandler {
    private val handleCmds = setOf("RPUSH", "LRANGE", "LPUSH", "LLEN")
    override fun isHandle(cmd: String): Boolean {
        return handleCmds.contains(cmd)
    }

    override fun handle(connection: Connection) {
        when (connection.cmd) {
            "RPUSH" -> rpush(connection)
            "LPUSH" -> lpush(connection)
            "LRANGE" -> lrange(connection)
            "LLEN" -> llen(connection)
        }
    }

    private fun rpush(connection: Connection) {
        val key = connection.args[0]
        val values = connection.args.drop(1)

        if (storageService.getList(key) == null) {
            storageService.setList(key, mutableListOf())
        }
        val list = storageService.getList(key)!!
        for (value in values) {
            list.add(value)
        }
        storageService.setList(key, list)
        commandResultWriter.writeInteger(connection, list.size)
    }

    private fun lpush(connection: Connection) {
        val key = connection.args[0]
        val values = connection.args.drop(1)

        if (storageService.getList(key) == null) {
            storageService.setList(key, mutableListOf())
        }
        val list = storageService.getList(key)!!
        for (value in values) {
            list.add(0, value)
        }
        storageService.setList(key, list)
        commandResultWriter.writeInteger(connection, list.size)
    }

    private fun lrange(connection: Connection) {
        val key = connection.args[0]
        var start = connection.args[1].toInt()
        var end = connection.args[2].toInt()

        val list = storageService.getList(key)
        if (list == null) {
            commandResultWriter.writeArrayOfBulkString(connection, emptyList())
            return
        }
        if (start < 0) {
            if (start * -1 > list.size) {
                start = 0
            } else {
                start += list.size
            }
        }
        if (end < 0) {
            if (end * -1 > list.size) {
                end = 0
            } else {
                end += list.size
            }
        }
        if (start >= list.size || start > end) {
            commandResultWriter.writeArrayOfBulkString(connection, emptyList())
            return
        }
        commandResultWriter.writeArrayOfBulkString(connection, list.subList(start, min(list.size, end + 1)))
    }

    private fun llen(connection: Connection) {
        val key = connection.args[0]
        val list = storageService.getList(key)
        commandResultWriter.writeInteger(connection, list?.size ?: 0)
    }
}
