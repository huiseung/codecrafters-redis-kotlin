package protocol

import Connection
import model.RESP
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

class CommandReader(
) {
    fun read(connection: Connection) {
        connection.init()
        val inputReader = connection.getStreamReader()
        val type = inputReader.read()
        if (type == -1) {
            return
        }
        val request = when (Char(type)) {
            RESP.array -> parseArray(inputReader)
            RESP.bulkString -> listOf(parseBulkString(inputReader))
            RESP.simpleString -> listOf(parseSimpleString(inputReader))
            else -> throw IllegalArgumentException()
        }
        connection.cmd = request[0].uppercase()
        if (request.size > 1) {
            connection.args = request.drop(1)
            connection.argCount = connection.args.size
        }
    }

    private fun parseArray(inputReader: InputStreamReader): MutableList<String> {
        val cmd: MutableList<String> = mutableListOf()
        val length = parseInt(inputReader)
        if (length == -1) {
            return cmd
        }
        repeat(length) {
            val type = inputReader.read()
            val data = when (Char(type)) {
                RESP.bulkString -> parseBulkString(inputReader)
                else -> throw IllegalArgumentException()
            }
            cmd.add(data)
        }
        return cmd
    }

    private fun parseBulkString(inputReader: InputStreamReader): String {
        val length = parseInt(inputReader)
        return parseUntilEndLine(inputReader)
    }

    private fun parseSimpleString(inputReader: InputStreamReader): String {
        return parseUntilEndLine(inputReader)
    }

    private fun parseInt(inputReader: InputStreamReader): Int {
        return Integer.parseInt(parseUntilEndLine(inputReader))
    }

    private fun parseUntilEndLine(inputReader: InputStreamReader): String {
        val sb = StringBuilder()

        var meetCR: Boolean = false
        while (true) {
            val value = inputReader.read()
            if (value == -1) {
                break
            }
            val ch = Char(value)
            if (ch == '\n' && meetCR) {
                break
            } else if (ch == '\r') {
                meetCR = true
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
