package protocol

import java.io.OutputStream

class Serializer(
    private val outputStream: OutputStream,
) {
    fun write(value: Any){
        if(value is String){
            writeSimpleString(value)
        }
        else if(value is BulkString){
            writeBulkString(value)
        }
        outputStream.flush()
    }

    private fun writeSimpleString(value: String){
        outputStream.write("${Resp.SIMPLE_STRING.value}${value}\r\n".toByteArray())
    }

    private fun writeBulkString(value: BulkString){
        val data = value.message.toByteArray()
        outputStream.write("${Resp.BULK_STRING.value}${data.size}\r\n".toByteArray())
        outputStream.write("${value.message}\r\n".toByteArray())
    }
}


