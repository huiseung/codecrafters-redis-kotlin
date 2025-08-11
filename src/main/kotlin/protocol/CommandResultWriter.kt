package protocol

import model.*
import network.Connection
import java.nio.charset.StandardCharsets.UTF_8

class CommandResultWriter(
) {

    fun writeSimpleString(connection: Connection, value: String) {
        val writer = connection.getBufferedWriter()
        writer.write("${RESP.simpleString}${value}${RESP.crlf}")
        writer.flush()
    }

    fun writeBulkString(connection: Connection, value: String) {
        val writer = connection.getBufferedWriter()
        writer.write("${RESP.bulkString}${value.toByteArray(UTF_8).size}${RESP.crlf}")
        writer.write("${value}${RESP.crlf}")
        writer.flush()
    }

    fun writeArrayOfBulkString(connection: Connection, values: List<String>){
        val writer = connection.getBufferedWriter()
        writer.write("${RESP.array}${values.size}${RESP.crlf}")
        for(value in values){
            writer.write("${RESP.bulkString}${value.toByteArray(UTF_8).size}${RESP.crlf}")
            writer.write("${value}${RESP.crlf}")
        }
        writer.flush()
    }

    fun writeEmptyArray(connection: Connection){
        val writer = connection.getBufferedWriter()
        writer.write("${RESP.array}0${RESP.crlf}")
        writer.flush()
    }

    fun writeNIL(connection: Connection) {
        val writer = connection.getBufferedWriter()
        writer.write("${RESP.bulkString}${RESP.nil}${RESP.crlf}")
        writer.flush()
    }

    fun writeInteger(connection: Connection, value: Int) {
        val writer = connection.getBufferedWriter()
        writer.write("${RESP.integer}${value}${RESP.crlf}")
        writer.flush()
    }

    fun writeError(connection: Connection, message: String?) {
        val writer = connection.getBufferedWriter()
        writer.write("${RESP.error}${message}${RESP.crlf}")
        writer.flush()
    }
}


