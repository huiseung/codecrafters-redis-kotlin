package handler

import java.lang.StringBuilder

class ResponseFormatter {
    fun format(str: String): String{
        return "+${str}\r\n"
    }

    fun format(array: List<String>): String {
        val sb = StringBuilder()
        sb.append("*").append("${array.size}").append("\r\n")
        for(str in array){
            sb.append("\$${str.toByteArray().size}\r\n$str\r\n")
        }
        return sb.toString()
    }
}
