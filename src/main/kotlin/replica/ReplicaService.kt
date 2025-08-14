package replica

import config.RedisConfig
import network.ConnectionCtx
import network.ConnectionType
import persistence.RdbManager
import protocol.Request
import protocol.RespWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

data class PendingWait(
    val connection: ConnectionCtx,
    val targetOffset: Long,
    val minReplica: Int,
    val deadlineMs: Long,
)

class ReplicaService(
    private val config: RedisConfig,
    private val respWriter: RespWriter,
    private val selector: Selector,
    private val rdbManager: RdbManager,
) {
    val replicas = mutableSetOf<ConnectionCtx>()
    var masterOffset: Long = 0L
    private val pendingWaits = ArrayDeque<PendingWait>()

    fun run() {
        if (config.get("role") != "slave") {
            return
        }
        val masterChannel = connectToMaster()
        masterChannel.configureBlocking(false)
        val key = masterChannel.register(selector, SelectionKey.OP_READ)
        key.attach(ConnectionCtx(key, ConnectionType.FOR_MASTER))
    }

    fun propagation(req: Request) {
        if (config.get("role") != "master") return
        val payload = respWriter.writeArrayOfBulkString(req.request)
        for (repl in replicas) {
            val dup = payload.duplicate()
            dup.position(0)
            dup.limit(payload.limit())
            repl.writeBuffer(dup)
        }
        masterOffset += req.bytes
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
        var line = readLineAscii(masterChannel) ?: return
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
        line = readLineAscii(masterChannel) ?: throw IllegalArgumentException()
        if (line != "+OK") return

        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("REPLCONF", "capa", "psync2")))
        line = readLineAscii(masterChannel) ?: throw IllegalArgumentException()
        if (line != "+OK") return

        masterChannel.write(respWriter.writeArrayOfBulkString(listOf("PSYNC", "?", "-1")))
        line = readLineAscii(masterChannel) ?: throw IllegalArgumentException()
        if (!line.startsWith("+FULLRESYNC")) return
        handlePsync(masterChannel)
    }

    private fun handlePsync(masterChannel: SocketChannel) {
        val dollar = readExactly(masterChannel, 1)[0]
        val lenLine = readLineAscii(masterChannel)
        val totalLen = lenLine.toLongOrNull() ?: return

        val rdbInput = object : InputStream() {
            var remaining = totalLen
            private val tmp = ByteBuffer.allocate(8192).apply {
                flip()
            }

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
                        val maxChunk = minOf(tmp.capacity().toLong(), remaining).toInt()
                        tmp.limit(maxChunk)
                        val n = masterChannel.read(tmp)
                        if (n < 0) return if (filled > 0) filled else -1
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

    fun offer(wait: PendingWait) {
        pendingWaits.add(wait)
    }

    fun checkWaits() {
        if (pendingWaits.isEmpty()) return
        val now = System.currentTimeMillis()
        val toProcess = ArrayList<PendingWait>(pendingWaits.size)
        while (pendingWaits.isNotEmpty()) {
            toProcess.add(pendingWaits.removeFirst())
        }
        for (pw in toProcess) {
            val count = replicas.count() { it.offset >= pw.targetOffset }
            val timeoutReached = now >= pw.deadlineMs
            if (count >= pw.minReplica || timeoutReached) {
                pw.connection.writeBuffer(respWriter.writeInteger(count))
                pw.connection.enableReadInterest()
            } else {
                pendingWaits.add(pw)
            }
        }
    }
}
