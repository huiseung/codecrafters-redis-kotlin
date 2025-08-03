package handler.command

import Connection
import StorageService
import protocol.CommandResultWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min


class ListCommandHandler(
    val storageService: StorageService,
    private val commandResultWriter: CommandResultWriter,
) : CommandHandler {
    private val handleCmds = setOf("RPUSH", "LRANGE", "LPUSH", "LLEN", "LPOP", "BLPOP")
    private val lock = ReentrantLock()
    private val queues = ConcurrentHashMap<String, Condition>()

    override fun isHandle(cmd: String): Boolean {
        return handleCmds.contains(cmd)
    }

    override fun handle(connection: Connection) {
        when (connection.cmd) {
            "RPUSH" -> rpush(connection)
            "LPUSH" -> lpush(connection)
            "LRANGE" -> lrange(connection)
            "LLEN" -> llen(connection)
            "LPOP" -> lpop(connection)
            "BLPOP" -> blpop(connection)
        }
    }

    private fun rpush(connection: Connection) {
        lock.lock()
        try {
            val key = connection.args[0]
            val values = connection.args.drop(1)
            val list = storageService.getOrCreateList(key)
            list.addAll(values)
            queues[key]?.signal()
            commandResultWriter.writeInteger(connection, list.size)
        } finally {
            lock.unlock()
        }
    }

    private fun lpush(connection: Connection) {
        val key = connection.args[0]
        val values = connection.args.drop(1)
        val list = storageService.getOrCreateList(key)
        for (value in values) {
            list.add(0, value)
        }
        commandResultWriter.writeInteger(connection, list.size)
    }

    private fun lrange(connection: Connection) {
        val key = connection.args[0]
        var start = connection.args[1].toInt()
        var end = connection.args[2].toInt()
        val list = storageService.getList(key)
        if (list == null) {
            commandResultWriter.writeEmptyArray(connection)
            return
        }
        if (start < 0) {
            start += list.size
            if (start < 0) {
                start = 0
            }
        }
        if (end < 0) {
            end += list.size
            if (end < 0) {
                end = 0
            }
        }
        if (start >= list.size) {
            commandResultWriter.writeEmptyArray(connection)
            return
        }
        end = min(end, list.size - 1)
        val ret = mutableListOf<String>()
        for (idx in start..end) {
            ret.add(list[idx])
        }
        commandResultWriter.writeArrayOfBulkString(connection, ret)
    }

    private fun llen(connection: Connection) {
        val key = connection.args[0]
        val list = storageService.getList(key)
        commandResultWriter.writeInteger(connection, list?.size ?: 0)
    }

    private fun lpop(connection: Connection) {
        val key = connection.args[0]
        val list = storageService.getList(key)
        if (list == null) {
            commandResultWriter.writeNIL(connection)
            return
        }
        val count = min(list.size, if (connection.argCount > 1) connection.args[1].toInt() else 1)
        val ret = mutableListOf<String>()
        repeat(count) {
            ret.add(list.pollFirst())
        }
        if (ret.size == 1) {
            commandResultWriter.writeBulkString(connection, ret[0])
            return
        }
        commandResultWriter.writeArrayOfBulkString(connection, ret)
    }

    private fun blpop(connection: Connection) {
        val key = connection.args[0]
        val timeoutMs = (connection.args[1].toDouble() * 1000).toLong()
        val startTime = System.currentTimeMillis()
        lock.lock()
        try {
            val list = storageService.getOrCreateList(key)
            if (list.isNotEmpty()) {
                val value = list.pollFirst()
                commandResultWriter.writeArrayOfBulkString(connection, listOf(key, value))
                return
            }
            val condition = queues.getOrPut(key) { lock.newCondition() }
            while (true) {
                val remainMs = if (timeoutMs == 0L) 0 else timeoutMs - (System.currentTimeMillis() - startTime)
                if (timeoutMs == 0L) {
                    condition.await()
                } else if (remainMs <= 0 || !condition.await(remainMs, TimeUnit.MILLISECONDS)) {
                    commandResultWriter.writeNIL(connection)
                    return
                }
                if (list.isNotEmpty()) {
                    val value = list.pollFirst()
                    commandResultWriter.writeArrayOfBulkString(connection, listOf(key, value))
                    return
                }
            }
        } finally {
            lock.unlock()
        }
    }
}
