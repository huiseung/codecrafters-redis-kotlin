package network

import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

enum class ConnectionType {
    FOR_NORMAL,
    FOR_MASTER,
    FOR_REPLICA,
}

class ConnectionCtx(
    val selectionKey: SelectionKey,
    var connectionType: ConnectionType,
    var readBuffer: ByteBuffer = ByteBuffer.allocate(64 * 1024),
    val writeBufferQueue: ArrayDeque<ByteBuffer> = ArrayDeque<ByteBuffer>(),
    var replOffset: Long = 0L,
) {
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

    fun plusReplOffset(consume: Int) {
        if (connectionType != ConnectionType.FOR_MASTER) return
        if (consume > 0) replOffset += consume.toLong()
    }
}
