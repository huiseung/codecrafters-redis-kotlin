package storage

class StorageService(
    private val db: MutableMap<String, Pair<String, Long?>> = mutableMapOf()
) {
    fun set(key: String, value: String, expirationMs: Long?=null){
        val expirationTime = expirationMs?.let { System.currentTimeMillis() + it}
        db[key] = Pair(value, expirationTime)
    }

    fun get(key: String): String?{
        val entry = db[key] ?: return null
        val value = entry.first
        val expirationTime = entry.second
        val currentTime = System.currentTimeMillis()
        expirationTime?.let{
            if(expirationTime <= currentTime){
                db.remove(key)
                return null
            }
        }
        return value
    }

}
