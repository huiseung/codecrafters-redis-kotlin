package protocol

import model.Resp
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

class DeSerializer(
    private val inputStream: InputStream,
    private val inputReader: InputStreamReader = InputStreamReader(inputStream)
) {
    fun read(): List<String>{
        val type = inputReader.read()
        if(type == -1){
            return emptyList()
        }
        return when(Char(type)){
            Resp.array -> parseArray()
            Resp.bulkString -> listOf(parseBulkString())
            Resp.simpleString -> listOf(parseSimpleString())
            else -> throw IllegalArgumentException()
        }
    }

    private fun parseArray(): MutableList<String>{
        val cmd: MutableList<String> = mutableListOf()
        val length = parseInt()
        if(length == -1){
            return cmd
        }
        repeat(length){
            val type = inputReader.read()
            val data = when(Char(type)){
                Resp.bulkString -> parseBulkString()
                else -> throw IllegalArgumentException()
            }
            cmd.add(data)
        }
        return cmd
    }

    private fun parseBulkString(): String{
        val length = parseInt()
        return parseUntilEndLine()
    }

    private fun parseSimpleString():String{
        return parseUntilEndLine()
    }

    private fun parseInt(): Int{
        return Integer.parseInt(parseUntilEndLine())
    }

    private fun parseUntilEndLine(): String{
        val sb = StringBuilder()

        var meetCR: Boolean = false
        while(true){
            val value = inputReader.read()
            if(value == -1){
                break
            }
            val ch = Char(value)
            if(ch == '\n' && meetCR){
                break
            }
            else if(ch == '\r'){
                meetCR = true
            }
            else{
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
