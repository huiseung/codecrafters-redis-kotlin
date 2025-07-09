import server.RedisServer

fun main(args: Array<String>) {
    System.err.println("Logs from your program will appear here!")
    val redisServer = RedisServer()
    redisServer.start()
}
