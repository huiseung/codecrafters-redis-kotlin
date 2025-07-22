import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class Handler(
    private val clientSocket: Socket
) {
    fun handle(){
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
