package protocol

import model.BulkString
import model.Nil
import model.Resp
import java.io.OutputStream

class Serializer(
    private val outputStream: OutputStream,
) {
    fun write(value: Any) {
        if (value is String) {
            writeSimpleString(value)
        } else if (value is BulkString) {
            writeBulkString(value)
        } else if (value is Nil) {
            writeNil(value)
        }
        outputStream.flush()
    }

    private fun writeSimpleString(value: String) {
        outputStream.write("${Resp.simpleString}${value}${Resp.crlf}".toByteArray())
    }

    private fun writeBulkString(value: BulkString) {
        val data = value.message.toByteArray()
        outputStream.write("${Resp.bulkString}${data.size}${Resp.crlf}".toByteArray())
        outputStream.write("${value.message}\r\n".toByteArray())
    }

    private fun writeNil(value: Nil) {
        outputStream.write("${Resp.simpleString}${Resp.nil}${Resp.crlf}".toByteArray())
    }
}


