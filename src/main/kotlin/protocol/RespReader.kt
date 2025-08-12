package protocol

import network.ConnectionCtx
import java.nio.ByteBuffer

class RespReader {
    fun parseRequests(connection: ConnectionCtx): List<List<String>> {
        val readBuffer = connection.readBuffer
        readBuffer.flip()
        val limit = readBuffer.limit()
        var cursor = readBuffer.position()
        val ret = mutableListOf<MutableList<String>>()
        while (true) {
            val (request, next) = parseArrayOfBulkString(readBuffer, cursor, limit) ?: break
            ret.add(request)
            cursor = next
        }
        readBuffer.position(cursor)
        readBuffer.compact()
        return ret
    }

    private fun parseArrayOfBulkString(readBuffer: ByteBuffer, from: Int, limit: Int): Pair<MutableList<String>, Int>? {
        var cursor = from
        val (data, next) = readLine(readBuffer, cursor, limit) ?: return null
        if (data.isEmpty() || !data.startsWith("*")) return null
        var size = data.substring(1).toInt()
        cursor = next
        val ret = mutableListOf<String>()
        var i = 0
        repeat(size) {
            var (string, next) = parseBulkString(readBuffer, cursor, limit) ?: return null
            if (i == 0) {
                string = string.uppercase()
            }
            i += 1
            ret.add(string)
            cursor = next
        }
        return ret to cursor
    }

    private fun parseBulkString(readBuffer: ByteBuffer, from: Int, limit: Int): Pair<String, Int>? {
        var cursor = from
        val (data, next) = readLine(readBuffer, cursor, limit) ?: return null
        if (!data.startsWith("$")) return null
        val length = data.substring(1).toInt()
        cursor = next
        val str = readLine(readBuffer, cursor, limit) ?: return null
        return str
    }

    private fun readLine(readBuffer: ByteBuffer, from: Int, limit: Int): Pair<String, Int>? {
        var i = from
        while (i < limit - 1) {
            if (readBuffer.get(i) == '\r'.code.toByte()
                && readBuffer.get(i + 1) == '\n'.code.toByte()
            ) {
                val length = i - from
                val bytes = ByteArray(length)
                for (k in 0 until length) {
                    bytes[k] = readBuffer.get(from + k)
                }
                return String(bytes) to (i + 2)
            }
            i += 1
        }
        return null
    }
}
