package model

data class Entry(
    val obj: RedisObject,
    val expireAt: Long?
)

sealed class RedisObject{
    data class RedisString(val value: String) : RedisObject()
    data class RedisList(val value: MutableList<String>): RedisObject()
}

