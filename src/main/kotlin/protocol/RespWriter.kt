package protocol

import model.RESP
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class RespWriter {
    fun writeSimpleString(data: String): ByteBuffer {
        val buffer = ByteBuffer.wrap("${RESP.simpleString}$data${RESP.crlf}".toByteArray(StandardCharsets.UTF_8))
        return buffer
    }

    fun writeBulkString(data: String): ByteBuffer {
        return ByteBuffer.wrap(
            "${RESP.bulkString}${data.toByteArray(StandardCharsets.UTF_8).size}${RESP.crlf}$data${RESP.crlf}".toByteArray(
                StandardCharsets.UTF_8
            )
        )
    }

    fun writeNil(): ByteBuffer {
        val buffer = ByteBuffer.wrap(
            "${RESP.bulkString}${RESP.nil}${RESP.crlf}".toByteArray(StandardCharsets.UTF_8)
        )
        return buffer
    }

    fun writeNilArray(): ByteBuffer{
        val buffer = ByteBuffer.wrap(
            "${RESP.array}${RESP.nil}${RESP.crlf}".toByteArray(StandardCharsets.UTF_8)
        )
        return buffer
    }

    fun writeInteger(int: Int): ByteBuffer {
        val buffer = ByteBuffer.wrap(
            "${RESP.integer}$int${RESP.crlf}".toByteArray(StandardCharsets.UTF_8)
        )
        return buffer
    }

    fun writeEmptyArray(): ByteBuffer {
        val buffer = ByteBuffer.wrap(
            "${RESP.array}0${RESP.crlf}".toByteArray(StandardCharsets.UTF_8)
        )
        return buffer
    }

    fun writeArrayOfBulkString(array: List<String>): ByteBuffer {
        var total = 0
        total += 1 /* '*' */ + array.size.toString().toByteArray(StandardCharsets.UTF_8).size + 2 /* \r\n */
        val valueBytesList = ArrayList<ByteArray>(array.size)
        for (v in array) {
            val vb = v.toByteArray(StandardCharsets.UTF_8)
            valueBytesList += vb
            total += 1 /* '$' */ + vb.size.toString().toByteArray(StandardCharsets.UTF_8).size + 2 /* \r\n */ // header
            total += vb.size + 2 /* value + \r\n */
        }

        // 2) 정확 크기 버퍼 할당
        val buf = ByteBuffer.allocate(total)

        // 3) 배열 헤더 "*<count>\r\n"
        buf.put(RESP.array.code.toByte())
        buf.put(array.size.toString().toByteArray(StandardCharsets.UTF_8))
        buf.put(RESP.crlf.toByteArray(StandardCharsets.UTF_8))

        // 4) 각 Bulk String: "$<len>\r\n<data>\r\n"
        for ((i, v) in array.withIndex()) {
            val vb = valueBytesList[i]
            buf.put(RESP.bulkString.code.toByte())
            buf.put(vb.size.toString().toByteArray(StandardCharsets.UTF_8))
            buf.put(RESP.crlf.toByteArray(StandardCharsets.UTF_8))
            buf.put(vb)
            buf.put(RESP.crlf.toByteArray(StandardCharsets.UTF_8))
        }

        buf.flip()
        return buf
    }
}
