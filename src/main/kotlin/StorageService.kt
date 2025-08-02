
import model.Entry
import model.RedisObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StorageService {
    private val db: MutableMap<String, Entry> = ConcurrentHashMap<String, Entry>()

    fun keys(pattern: String): List<String> {
        val regexPattern = pattern.replace("*", ".*")
        val regex = Regex("^$regexPattern$")
        return db.keys.filter { it.matches(regex) }.toList()
    }

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
        db[key] = Entry(RedisObject.RedisString(value), expiryTime)
    }

    fun getList(key: String): LinkedList<String>? {
        val entry = db[key] ?: return null
        val obj = entry.obj as RedisObject.RedisList
        return obj.value
    }

    fun getOrCreateList(key: String): LinkedList<String> {
        return db.computeIfAbsent(key) {
            Entry(RedisObject.RedisList(LinkedList()), null)
        }.let {
            (it.obj as RedisObject.RedisList).value
        }
    }
}
