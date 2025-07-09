package handler

import java.lang.UnsupportedOperationException

class BasicCommandHandler(
    private val responseFormatter: ResponseFormatter = ResponseFormatter()
): CommandHandler {
    override fun isHandle(cmd: List<String>): Boolean {
        return when(cmd[0].uppercase()){
            "PING" -> true
            else -> false
        }
    }

    override fun handle(cmd: List<String>): String {
        return when(cmd[0].uppercase()){
            "PING" -> ping()
            else -> throw UnsupportedOperationException()
        }
    }

    private fun ping() = responseFormatter.format("PONG")

}
