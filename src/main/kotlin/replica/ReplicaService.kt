package replica

import config.RedisConfig
import network.ConnectionCtx
import network.ConnectionType
import persistence.RdbManager
import protocol.RespWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class ReplicaService(
    private val config: RedisConfig,
    private val respWriter: RespWriter,
    private val selector: Selector,
    private val rdbManager: RdbManager,
) {
    private val replicas = mutableSetOf<ConnectionCtx>()

    fun run() {
        if (config.get("role") != "slave") {
            return
        }
        val masterChannel = connectToMaster()
        masterChannel.configureBlocking(false)
        val key = masterChannel.register(selector, SelectionKey.OP_READ)
        key.attach(ConnectionCtx(key, ConnectionType.FOR_MASTER))
    }

    fun propagation(req: List<String>) {
        if (config.get("role") != "master") return
        val payload = respWriter.writeArrayOfBulkString(req)
        for (repl in replicas) {
            val dup = payload.duplicate()
            dup.position(0)
            dup.limit(payload.limit())
            repl.writeBuffer(dup)
        }
    }

    fun registerReplica(connectionCtx: ConnectionCtx) {
        if (config.get("role") == "master") {
            replicas += connectionCtx
        }
    }

    private fun connectToMaster(): SocketChannel {
        if (config.get("role") == "master") {
            throw IllegalArgumentException()
        }
        val socketAddress = InetSocketAddress(config.get("master_host"), config.get("master_port")!!.toInt())
        val masterChannel = SocketChannel.open(socketAddress)
        masterChannel.configureBlocking(true)
        handshake(masterChannel)
        return masterChannel
    }

    private fun handshake(masterChannel: SocketChannel) {
        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("PING")))
        var line = readSimpleLine(masterChannel) ?: return
        if (line != "+PONG") return

        masterChannel.write(
            respWriter.writeArrayOfBulkString(
                listOf(
                    "REPLCONF",
                    "listening-port",
                    "${config.get("port")}"
                )
            )
        )
        line = readSimpleLine(masterChannel) ?: throw IllegalArgumentException()
        if (line != "+OK") return

        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("REPLCONF", "capa", "psync2")))
        line = readSimpleLine(masterChannel) ?: throw IllegalArgumentException()
        if (line != "+OK") return

        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("PSYNC", "?", "-1")))
        line = readSimpleLine(masterChannel) ?: throw IllegalArgumentException()
        if (!line.startsWith("+FULLRESYNC")) return
        handlePsync(masterChannel)
    }

    private fun handlePsync(masterChannel: SocketChannel) {
        val dollar = readExactly(masterChannel, 1)[0]
        val lenLine = readLineAscii(masterChannel)
        val totalLen = lenLine.toLongOrNull() ?: return

        val rdbInput = object : InputStream() {
            var remaining = totalLen
            private val tmp = ByteBuffer.allocate(8192)
            override fun read(): Int {
                val one = ByteArray(1)
                val n = read(one, 0, 1)
                return if (n <= 0) -1 else one[0].toInt() and 0xFF
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                var toRead = minOf(len.toLong(), remaining).toInt()
                var filled = 0
                while (toRead > 0) {
                    if (!tmp.hasRemaining()) {
                        tmp.clear()
                        val n = masterChannel.read(tmp)
                        if (n < 0) error("EOF from master while receiving RDB")
                        if (n == 0) continue
                        tmp.flip()
                    }
                    val n = minOf(tmp.remaining(), toRead)
                    tmp.get(b, off + filled, n)
                    filled += n
                    toRead -= n
                    remaining -= n
                }
                return filled
            }
        }

        rdbManager.loadFromStream(rdbInput)
    }

    private fun readSimpleLine(ch: SocketChannel): String? {
        val buf = ByteBuffer.allocate(8 * 1024)
        val out = StringBuilder()
        while (true) {
            buf.clear()
            val n = ch.read(buf)
            if (n <= 0) return null
            buf.flip()
            val s = StandardCharsets.UTF_8.decode(buf).toString()
            out.append(s)
            val idx = out.indexOf("\r\n")
            if (idx >= 0) {
                return out.substring(0, idx)
            }
        }
    }

    private fun readExactly(ch: SocketChannel, n: Int): ByteArray {
        val out = ByteArray(n)
        var off = 0
        val buf = ByteBuffer.allocate(n)
        while (off < n) {
            val r = ch.read(buf)
            if (r < 0) return out
            if (r == 0) continue
            buf.flip()
            val got = buf.remaining()
            buf.get(out, off, got)
            off += got
            buf.clear()
        }
        return out
    }

    private fun readLineAscii(ch: SocketChannel): String {
        val baos = ByteArrayOutputStream()
        var prev = -1
        val one = ByteBuffer.allocate(1)
        while (true) {
            one.clear()
            val r = ch.read(one)
            if (r < 0) return ""
            if (r == 0) continue
            one.flip()
            val b = one.get().toInt() and 0xFF
            if (prev == '\r'.code && b == '\n'.code) {
                val bytes = baos.toByteArray()
                return String(bytes, 0, bytes.size - 1, Charsets.US_ASCII)
            }
            baos.write(b)
            prev = b
        }
    }
}
