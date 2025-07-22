import model.BulkString
import model.Nil
import protocol.DeSerializer
import protocol.Serializer
import java.net.Socket

class Handler(
    private val clientSocket: Socket,
    private val storageService: StorageService = StorageService(),
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
                        "SET" -> set(request)
                        "GET" -> get(request)
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

    private fun set(request: List<String>): String{
        if(request.size == 5 && request[3].uppercase() == "PX"){
            storageService.set(request[1], request[2], request[4].toLong())
        }else{
            storageService.set(request[1], request[2], null)
        }
        return "OK"
    }

    private fun get(request: List<String>): Any{
        val value = storageService.get(request[1])
        if(value is String){
            return BulkString(value)
        }
        return Nil()
    }
}
