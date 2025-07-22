package handler

import StorageService
import model.RespInteger
import model.Nil

class ListCommandHandler(
    val storageService: StorageService
) {
    fun rpush(request: List<String>): RespInteger {
        val key = request[1]
        val pastList = storageService.get(key)
        val newList: MutableList<String> = if (pastList is Nil) {
            val ret = mutableListOf<String>()
            for(idx in 2 until request.size){
               ret.add(request[idx])
            }
            ret
        } else if (pastList is MutableList<*>) {
            val ret = pastList as MutableList<String>
            for(idx in 2 until request.size){
                ret.add(request[idx])
            }
            ret
        } else {
            mutableListOf()
        }
        storageService.set(key, newList, null)
        return RespInteger(newList.size)
    }
}
