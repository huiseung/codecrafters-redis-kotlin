import model.Entry
import model.Nil
import model.RedisObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StorageService {
    private val db: MutableMap<String, Entry> = ConcurrentHashMap<String, Entry>()

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

    fun setList(key: String, value: LinkedList<String>) {
        db[key] = Entry(RedisObject.RedisList(value), null)
    }

    fun deleteList(key: String){
        db.remove(key)
    }
}
