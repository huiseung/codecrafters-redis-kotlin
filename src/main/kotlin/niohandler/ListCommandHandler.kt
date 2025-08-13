package niohandler

import network.ConnectionCtx
import protocol.RespWriter
import storage.StorageService
import storage.Waiter
import storage.WaiterService
import kotlin.math.min

class ListCommandHandler(
    private val storageService: StorageService,
    private val waiterService: WaiterService,
    private val respWriter: RespWriter,
) : CommandHandler {
    private val handleCmds = setOf("RPUSH", "LRANGE", "LPUSH", "LLEN", "LPOP", "BLPOP")

    override fun isHandle(cmd: String): Boolean {
        return handleCmds.contains(cmd)
    }

    override fun handle(connection: ConnectionCtx, request: List<String>) {
        val cmd = request[0]
        val args = request.drop(1)
        when (cmd) {
            "RPUSH" -> rpush(connection, args)
            "LPUSH" -> lpush(connection, args)
            "LRANGE" -> lrange(connection, args)
            "LLEN" -> llen(connection, args)
            "LPOP" -> lpop(connection, args)
            "BLPOP" -> blpop(connection, args)
        }
    }

    private fun rpush(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        val values = args.drop(1)
        val list = storageService.getOrCreateList(key)
        list.addAll(values)
        connection.writeBuffer(respWriter.writeInteger(list.size))
        deliverToWaiters(key)
    }

    private fun lpush(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        val values = args.drop(1)
        val list = storageService.getOrCreateList(key)
        for (value in values) {
            list.add(0, value)
        }
        connection.writeBuffer(respWriter.writeInteger(list.size))
    }

    private fun lrange(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        var start = args[1].toInt()
        var end = args[2].toInt()
        val list = storageService.getList(key)
        if (list == null) {
            connection.writeBuffer(respWriter.writeEmptyArray())
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
            connection.writeBuffer(respWriter.writeEmptyArray())
            return
        }
        end = min(end, list.size - 1)
        val ret = mutableListOf<String>()
        for (idx in start..end) {
            ret.add(list[idx])
        }
        connection.writeBuffer(respWriter.writeArrayOfBulkString(ret))
    }

    private fun llen(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        val list = storageService.getList(key)
        connection.writeBuffer(respWriter.writeInteger(list?.size ?: 0))
    }

    private fun lpop(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        val list = storageService.getList(key)
        if (list == null) {
            connection.writeBuffer(respWriter.writeNil())
            return
        }
        val count = min(list.size, if (args.size > 1) args[1].toInt() else 1)
        val ret = mutableListOf<String>()
        repeat(count) {
            ret.add(list.pollFirst())
        }
        if (ret.size == 1) {
            connection.writeBuffer(respWriter.writeBulkString(ret[0]))
            return
        }
        connection.writeBuffer(respWriter.writeArrayOfBulkString(ret))
    }

    private fun blpop(connection: ConnectionCtx, args: List<String>) {
        val key = args[0]
        val timeoutMs = (args[1].toDouble() * 1000).toLong()
        val list = storageService.getList(key)
        if (!list.isNullOrEmpty()) {
            val value = list.pollFirst()
            connection.writeBuffer(respWriter.writeArrayOfBulkString(listOf(key, value)))
            return
        }
        val now = System.currentTimeMillis()
        val remains = if (timeoutMs == 0L) null else now + timeoutMs
        waiterService.register(key, Waiter(connection, remains))
        connection.disableReadInterest()
    }

    private fun deliverToWaiters(key: String) {
        val list = storageService.getList(key)
        if (list.isNullOrEmpty()) return

        val q = waiterService.getQueue(key) ?: return
        while (list.isNotEmpty() && q.isNotEmpty()) {
            val waiter = q.removeFirst()
            val value = list.removeFirst()
            waiter.connection.writeBuffer(respWriter.writeArrayOfBulkString(listOf(key, value)))
            waiter.connection.enableReadInterest()
        }
        if (q.isEmpty()) {
            waiterService.removeQueue(key)
        }
    }
}
