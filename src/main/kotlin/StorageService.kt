import model.Entry
import model.Nil
import java.util.concurrent.ConcurrentHashMap

class StorageService {
    private val db: MutableMap<String, Entry> = ConcurrentHashMap<String, Entry>()

    fun get(key: String): Any{
        val entry = db[key] ?: return Nil()
        entry.expiry?.let{
            if(it < System.currentTimeMillis()){
                db.remove(key)
                return Nil()
            }
        }
        return entry.value
    }

    fun set(key: String, value: Any, expiryTime: Long?) {
        db[key] = Entry(value, expiryTime?.let { it + System.currentTimeMillis() })
    }
}
