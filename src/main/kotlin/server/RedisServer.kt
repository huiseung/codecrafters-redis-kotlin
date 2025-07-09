package server

import handler.RequestHandler
import java.net.ServerSocket
import kotlin.concurrent.thread

class RedisServer(
    private val requestHandler: RequestHandler = RequestHandler(),
) {
    fun start(){
        var serverSocket = ServerSocket(6379)

        // Since the tester restarts your program quite often, setting SO_REUSEADDR
        // ensures that we don't run into 'Address already in use' errors
        serverSocket.reuseAddress = true

        while(true){
            val client = serverSocket.accept()
            thread {
                requestHandler.service(client)
            }
        }
    }
}
