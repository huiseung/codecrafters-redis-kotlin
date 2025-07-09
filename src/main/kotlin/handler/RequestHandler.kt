package handler

import java.lang.UnsupportedOperationException
import java.net.Socket

class RequestHandler(
    private val requestParser: RequestParser = RequestParser(),
) {
    private val handlers = listOf<CommandHandler>(
        BasicCommandHandler(),
    )

    fun service(socket: Socket){
        while(!socket.isClosed){
            try{
                val request = requestParser.parse(socket)
                val handler = findHandler(request)
                val response = handler.handle(request)
                writeResponse(socket, response)
            }catch (e: Exception){
                writeResponse(socket, "-ERROR")
            }
        }
    }

    private fun findHandler(request: List<String>): CommandHandler{
        for(handler in handlers){
            if(handler.isHandle(request)){
                return handler
            }
        }
        throw UnsupportedOperationException()
    }

    private fun writeResponse(socket: Socket, data: String){
        val writer = socket.getOutputStream()
        writer.write(data.toByteArray())
        writer.flush()
    }
}
