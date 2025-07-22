package handler

import StorageService
import model.RespInteger
import model.Nil

class ListCommandHandler(
    val storageService: StorageService
) {
    fun rpush(request: List<String>): RespInteger {
        val key = request[1]
        val value = request[2]
        val pastList = storageService.get(key)
        val newList: MutableList<String> = if (pastList is Nil) {
            mutableListOf(value)
        } else if (pastList is MutableList<*>) {
            val ret = pastList as MutableList<String>
            ret.add(value)
            ret
        } else {
            mutableListOf()
        }
        storageService.set(key, newList, null)
        return RespInteger(newList.size)
    }
}
