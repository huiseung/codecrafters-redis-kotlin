package storage

class StorageService(
    private val db: MutableMap<String, String> = mutableMapOf()
) {
    fun set(key: String, value: String){
        db[key] = value
    }

    fun get(key: String): String?{
        return db[key]
    }

}
