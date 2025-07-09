package handler

import java.lang.StringBuilder

class ResponseFormatter {
    fun formatSimpleString(str: String): String{
        return "+${str}\r\n"
    }

    fun formatBulkString(str: String): String{
        return "\$${str.toByteArray().size}\r\n$str\r\n"
    }

    fun formatBulkStringArray(array: List<String>): String {
        val sb = StringBuilder()
        sb.append("*").append("${array.size}").append("\r\n")
        for(str in array){
            sb.append(formatBulkString(str))
        }
        return sb.toString()
    }
}
