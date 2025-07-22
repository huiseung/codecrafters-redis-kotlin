package protocol

enum class Resp(
    val value: Char
) {
    ARRAY('*'),
    SIMPLE_STRING('+'),
    BULK_STRING('$'),
    INT(':'),
    ERROR('-'),
}
