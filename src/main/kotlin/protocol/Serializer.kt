package protocol

import model.*
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
        } else if(value is RespInteger){
            writeInteger(value)
        } else if(value is RespError){
            writeError(value)
        }
        outputStream.flush()
    }

    private fun writeSimpleString(value: String) {
        outputStream.write("${RESP.simpleString}${value}${RESP.crlf}".toByteArray())
    }

    private fun writeBulkString(value: BulkString) {
        val data = value.message.toByteArray()
        outputStream.write("${RESP.bulkString}${data.size}${RESP.crlf}".toByteArray())
        outputStream.write("${value.message}\r\n".toByteArray())
    }

    private fun writeNil(value: Nil) {
        outputStream.write("${RESP.bulkString}${RESP.nil}${RESP.crlf}".toByteArray())
    }

    private fun writeInteger(value: RespInteger){
        outputStream.write("${RESP.integer}${value.value}${RESP.crlf}".toByteArray())
    }

    private fun writeError(value: RespError){
        outputStream.write("${RESP.error}${value.message}${RESP.crlf}".toByteArray())
    }
}


