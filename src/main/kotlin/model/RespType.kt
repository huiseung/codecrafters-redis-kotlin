package model

data class BulkString(val message: String)

class Nil()


data class RespError(val message: String) {
}


data class RespInteger(val value: Int)


object RESP {
    const val crlf: String = "\r\n"
    const val nil: String = "-1"

    const val simpleString: Char = '+'
    const val bulkString: Char = '$'
    const val integer: Char = ':'
    const val array: Char = '*'
    const val error: Char = '-'
}
