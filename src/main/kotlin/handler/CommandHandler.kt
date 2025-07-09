package handler

interface CommandHandler {
    fun isHandle(cmd: List<String>): Boolean
    fun handle(cmd: List<String>): String
}
