package handler

import storage.StorageService
import java.lang.UnsupportedOperationException

class StringCommandHandler(
    private val responseFormatter: ResponseFormatter,
    private val storageService: StorageService,
): CommandHandler {
    override fun isHandle(cmd: List<String>): Boolean {
        return when(cmd[0].uppercase()){
            in setOf("GET", "SET") -> true
            else -> false
        }
    }

    override fun handle(cmd: List<String>): String {
        return when(cmd[0].uppercase()){
            "SET" -> set(cmd)
            "GET" -> get(cmd)
            else -> throw UnsupportedOperationException()
        }
    }

    private fun set(cmd: List<String>): String{
        val key = cmd[1]
        val value = cmd[2]
        var expirationMs: Long? = null
        if(cmd.size == 5 && cmd[3].uppercase() == "PX"){
            expirationMs = cmd[4].toLong()
        }
        storageService.set(key, value, expirationMs)
        return responseFormatter.formatSimpleString("OK")
    }

    private fun get(cmd: List<String>): String{
        val value = storageService.get(cmd[1]) ?: return responseFormatter.nilBulkString()
        return responseFormatter.formatBulkString(value)
    }

}
