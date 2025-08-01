import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import model.Entry
import model.RedisObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StorageService {
    private val db: MutableMap<String, Entry> = ConcurrentHashMap<String, Entry>()
    private val mutexes = ConcurrentHashMap<String, Mutex>()
    private val queues = ConcurrentHashMap<String, LinkedList<CompletableDeferred<Pair<String, String>>>>()

    fun getString(key: String): String? {
        val entry = db[key] ?: return null
        entry.expireAt?.let {
            if (it < System.currentTimeMillis()) {
                db.remove(key)
                return null
            }
        }
        val obj = entry.obj as RedisObject.RedisString
        return obj.value
    }

    fun setString(key: String, value: String, expiryTime: Long?) {
        db[key] = Entry(RedisObject.RedisString(value), expiryTime?.let { it + System.currentTimeMillis() })
    }

    fun getList(key: String): LinkedList<String>? {
        val entry = db[key] ?: return null
        val obj = entry.obj as RedisObject.RedisList
        return obj.value
    }

    suspend fun blpop(key: String, timeoutMs: Long): Pair<String, String>? {
        val mutex = mutexes.computeIfAbsent(key) { Mutex() }
        mutex.withLock {
            val list = getList(key)
            if (!list.isNullOrEmpty()) {
                val value = list.pollFirst()
                if (list.isEmpty()) {
                    db.remove(key)
                }
                return Pair(key, value)
            }
        }

        val deferred = CompletableDeferred<Pair<String, String>>()
        val queue = queues.computeIfAbsent(key) { LinkedList() }
        queue.add(deferred)

        return if (timeoutMs == 0L) {
            println("wait! ${queue.size}")
            deferred.await()
        } else {
            withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }
        }.also { ret ->
            queue.forEach { it ->
                println("{isActive: ${it.isActive}, isCompleted: ${it.isCompleted}, isCancle: ${it.isCancelled}}")
            }
            if (ret == null) {
                mutex.withLock {
                    println("cancle")
                    queues[key]?.removeIf { it == deferred }
                    if (queues[key]?.isEmpty() == true) {
                        queues.remove(key)
                    }
                }
            }
        }
    }

    private fun getOrCreateList(key: String): LinkedList<String> {
        return db.computeIfAbsent(key) {
            Entry(RedisObject.RedisList(LinkedList()), null)
        }.let {
            (it.obj as RedisObject.RedisList).value
        }
    }

    suspend fun lpush(key: String, values: List<String>): Int {
        val mutex = mutexes.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            val list = getOrCreateList(key)
            for (value in values) {
                list.add(0, value)
            }
            list.size
        }
    }

    suspend fun rpush(key: String, values: List<String>): Int {
        val mutex = mutexes.computeIfAbsent(key) { Mutex() }
        val size = mutex.withLock {
            val list = getOrCreateList(key)
            list.addAll(values)
            val size = list.size

            val queue = queues[key]
            val deferred = queue?.poll()
            if (queue != null && queue.isEmpty()) {
                queues.remove(key)
            }
            if (deferred != null) {
                val value = list.pollFirst()
                if (value != null) {
                    deferred.complete(Pair(key, value))
                }
            }
            size
        }
        return size
    }

    suspend fun lpop(key: String, count: Int): List<String>? {
        val mutex = mutexes.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            val list = getList(key) ?: return null
            val popped = mutableListOf<String>()
            repeat(count.coerceAtMost(list.size)) {
                popped.add(list.removeFirst())
            }
            if (list.isEmpty()) db.remove(key)
            popped
        }
    }

    suspend fun llen(key: String): Int {
        val mutex = mutexes.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            getList(key)?.size ?: 0
        }
    }

    suspend fun lrange(key: String, start: Int, end: Int): List<String> {
        val mutex = mutexes.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            val list = getList(key) ?: return emptyList()
            val from = if (start < 0) list.size + start else start
            val to = if (end < 0) list.size + end else end
            if (from > to || from >= list.size) return emptyList()
            list.subList(from.coerceAtLeast(0), (to + 1).coerceAtMost(list.size)).toList()
        }
    }
}
