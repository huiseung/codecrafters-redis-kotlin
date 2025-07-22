import protocol.BulkString
import protocol.DeSerializer
import protocol.Serializer
import java.net.Socket

class Handler(
    private val clientSocket: Socket,
) {
    fun handle(){
        clientSocket.use{
            val deSerializer: DeSerializer = DeSerializer(clientSocket.getInputStream())
            val serializer: Serializer = Serializer(clientSocket.getOutputStream())
            while(true){
                val request = deSerializer.read()
                if(request.isEmpty()){
                    break
                }
                try{
                    val data: Any = when(request[0].uppercase()){
                        "PING" -> ping()
                        "ECHO" -> echo(request)
                        else -> "-ERROR\r\nUnSupport\r\nCommand"
                    }
                    serializer.write(data)
                }catch (e: Exception){
                    serializer.write(e.message ?: "")
                }
            }
        }
    }

    private fun ping() = "PONG"
    private fun echo(request: List<String>) = BulkString(request[1])
}
