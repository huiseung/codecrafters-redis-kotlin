package model

import java.util.*

data class Entry(
    val obj: RedisObject,
    val expireAt: Long?
)

sealed class RedisObject{
    data class RedisString(val value: String) : RedisObject()
    data class RedisList(val value: LinkedList<String>): RedisObject()
}

