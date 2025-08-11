package network

import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

class Connection(
    val clientSocket: Socket,
    var connectionType: ConnectionType,

    var argCount: Int = -1,
    var cmd: String = "",
    var args: List<String> = mutableListOf(),
) {
    private val _inputStream: InputStream by lazy {
        clientSocket.getInputStream()
    }

    private val _streamReader: InputStreamReader by lazy {
        InputStreamReader(_inputStream)
    }

    private val outputStream: OutputStream by lazy {
        clientSocket.getOutputStream()
    }

    private val _bufferedWriter: BufferedWriter by lazy {
        outputStream.bufferedWriter()
    }

    fun getStreamReader(): InputStreamReader = _streamReader

    fun getBufferedWriter(): BufferedWriter = _bufferedWriter

    fun init(){
        argCount = -1
        cmd = ""
        args = mutableListOf()
    }
}

enum class ConnectionType {
    TO_NORMAL,
    TO_MASTER,
    FOR_FULLSYNC
}
