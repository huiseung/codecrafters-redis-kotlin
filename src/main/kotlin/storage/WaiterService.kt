package storage

import network.ConnectionCtx


data class Waiter(
    val connection: ConnectionCtx,
    val remains: Long?,
)

class WaiterService {
    private val queues: MutableMap<String, ArrayDeque<Waiter>> = mutableMapOf()

    fun register(key: String, waiter: Waiter) {
        queues.getOrPut(key) { ArrayDeque() }.add(waiter)
    }

    fun getQueue(key: String) = queues[key]

    fun removeQueue(key: String) {
        queues.remove(key)
    }

    fun expireAll() {
        if (queues.isEmpty()) return
        val now = System.currentTimeMillis()
        val keys = queues.keys.toList()
        for (key in keys) {
            val q = queues[key] ?: continue
            val it = q.iterator()
            while (it.hasNext()) {
                val waiter = it.next()
                if (waiter.remains != null && waiter.remains <= now) {
                    it.remove()
                    waiter.connection.writeNil()
                    waiter.connection.enableReadInterest()
                }
            }
            if (q.isEmpty()) {
                queues.remove(key)
            }
        }
    }
}
