package handler

import java.io.BufferedInputStream
import java.net.Socket

class RequestParser {
    fun parse(socket: Socket): List<String>{
        val reader = BufferedInputStream(socket.getInputStream()) // byte 단위 읽기

        var type = reader.read().toChar()
        require(type == '*')

        val count = readLine(reader).toInt()
        val ret = mutableListOf<String>()
        repeat(count){
            type = reader.read().toChar()
            require(type == '$')
            val length = readLine(reader)
            val content = readLine(reader)
            ret.add(content)
        }
        return ret
    }

    fun readLine(reader: BufferedInputStream): String {
        val sb = StringBuilder()
        while (true) {
            val ch = reader.read() // 한 바이트 읽기
            if (ch == -1) break
            if (ch.toChar() == '\r') {
                if (reader.read().toChar() == '\n') break
            } else {
                sb.append(ch.toChar())
            }
        }
        return sb.toString()
    }
}
