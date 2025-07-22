import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!")

    // Uncomment this block to pass the first stage
    val serverSocket = ServerSocket(6379)
    //
    // // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // // ensures that we don't run into 'Address already in use' errors
    serverSocket.use {
        serverSocket.reuseAddress = true
        val clientSocket = serverSocket.accept()
        clientSocket.use{
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(input))

            while(true){
                val request = reader.readLine() ?: break
                if(request.equals("PING", ignoreCase = true)){
                    output.write("+PONG\r\n".toByteArray())
                    output.flush()
                }
            }
        }

    }

}
