package network

import model.RESP
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.charset.StandardCharsets.UTF_8

enum class ConnectionType {
    FOR_NORMAL,
    FOR_MASTER,
    FOR_FULLSYNC
}

class ConnectionCtx(
    val selectionKey: SelectionKey,
    var connectionType: ConnectionType,
    var readBuffer: ByteBuffer = ByteBuffer.allocate(64 * 1024),
    val writeBufferQueue: ArrayDeque<ByteBuffer> = ArrayDeque<ByteBuffer>(),
) {
    private var pendingBytes: Int = 0

    fun feed(shareReadBuffer: ByteBuffer) {
        shareReadBuffer.flip()
        ensureCapa(shareReadBuffer.remaining())
        readBuffer.put(shareReadBuffer)
    }

    private fun ensureCapa(add: Int) {
        if (readBuffer.remaining() >= add) {
            return
        }
        val need = readBuffer.position() + add
        var cap = readBuffer.capacity()
        while (cap < need) {
            cap = cap shl 1
        }
        val newBuffer = ByteBuffer.allocate(cap)
        readBuffer.flip()
        newBuffer.put(readBuffer)
        readBuffer = newBuffer
    }

    fun writeSimpleString(data: String) {
        val buffer = ByteBuffer.wrap("${RESP.simpleString}$data${RESP.crlf}".toByteArray(UTF_8))
        writeBuffer(buffer)
    }

    fun writeBulkString(data: String) {
        val buffer = ByteBuffer.wrap(
            "${RESP.bulkString}${data.toByteArray(UTF_8).size}${RESP.crlf}$data${RESP.crlf}".toByteArray(UTF_8)
        )
        writeBuffer(buffer)
    }

    fun writeNil() {
        val buffer = ByteBuffer.wrap(
            "${RESP.bulkString}${RESP.nil}${RESP.crlf}".toByteArray(UTF_8)
        )
        writeBuffer(buffer)
    }

    fun writeInteger(int: Int) {
        val buffer = ByteBuffer.wrap(
            "${RESP.integer}$int${RESP.crlf}".toByteArray(UTF_8)
        )
        writeBuffer(buffer)
    }

    fun writeEmptyArray() {
        val buffer = ByteBuffer.wrap(
            "${RESP.array}0${RESP.crlf}".toByteArray(UTF_8)
        )
        writeBuffer(buffer)
    }

    fun writeArrayOfBulkString(array: List<String>) {
        var total = 0
        total += 1 /* '*' */ + array.size.toString().toByteArray(UTF_8).size + 2 /* \r\n */
        val valueBytesList = ArrayList<ByteArray>(array.size)
        for (v in array) {
            val vb = v.toByteArray(UTF_8)
            valueBytesList += vb
            total += 1 /* '$' */ + vb.size.toString().toByteArray(UTF_8).size + 2 /* \r\n */ // header
            total += vb.size + 2 /* value + \r\n */
        }

        // 2) 정확 크기 버퍼 할당
        val buf = ByteBuffer.allocate(total)

        // 3) 배열 헤더 "*<count>\r\n"
        buf.put(RESP.array.code.toByte())
        buf.put(array.size.toString().toByteArray(UTF_8))
        buf.put(RESP.crlf.toByteArray(UTF_8))

        // 4) 각 Bulk String: "$<len>\r\n<data>\r\n"
        for ((i, v) in array.withIndex()) {
            val vb = valueBytesList[i]
            buf.put(RESP.bulkString.code.toByte())
            buf.put(vb.size.toString().toByteArray(UTF_8))
            buf.put(RESP.crlf.toByteArray(UTF_8))
            buf.put(vb)
            buf.put(RESP.crlf.toByteArray(UTF_8))
        }

        buf.flip()
        writeBuffer(buf)
    }

    fun writeBuffer(buffer: ByteBuffer) {
        writeBufferQueue.add(buffer)
        selectionKey.interestOps(selectionKey.interestOps() or SelectionKey.OP_WRITE)
    }

    fun disableReadInterest() {
        val cur = selectionKey.interestOps()
        if ((cur and SelectionKey.OP_READ) != 0) {
            selectionKey.interestOps(cur and SelectionKey.OP_READ.inv())
        }
    }

    fun enableReadInterest() {
        val cur = selectionKey.interestOps()
        selectionKey.interestOps(cur or SelectionKey.OP_READ)
    }


}
