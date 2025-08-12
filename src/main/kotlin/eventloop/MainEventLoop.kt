package eventloop

import config.RedisConfig
import network.ConnectionCtx
import network.ConnectionType
import niohandler.CommandHandler
import protocol.RespReader
import storage.WaiterService
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class MainEventLoop(
    private val config: RedisConfig,
    private val commandHandlers: List<CommandHandler>,
    private val waiterService: WaiterService,
    private val selector: Selector = Selector.open(),
    private val serverChannel: ServerSocketChannel = ServerSocketChannel.open(),
    private val shareReadBuffer: ByteBuffer = ByteBuffer.allocateDirect(64 * 1024),
    private val respReader: RespReader = RespReader()
) {
    fun init() {
        serverChannel.configureBlocking(false);
        serverChannel.bind(InetSocketAddress("127.0.0.1", config.get("port")!!.toInt()))
        serverChannel.register(selector, SelectionKey.OP_ACCEPT)
    }

    fun run() {
        while (!Thread.currentThread().isInterrupted) {
            selector.select(10) // 이벤트 발생 상관없이 selector가 깨어나는 주기
            val iter = selector.selectedKeys().iterator()
            while (iter.hasNext()) {
                val key = iter.next()
                iter.remove()
                handleKey(key)
            }
            waiterService.expireAll()
        }
    }


    private fun handleKey(key: SelectionKey) {
        try {
            when {
                key.isAcceptable -> handleAccept(key)
                key.isReadable -> handleRead(key)
                key.isWritable -> handleWrite(key)
            }
        } catch (e: Exception) {
            println("handleKeys: ${e.message}")
        }
    }

    private fun handleAccept(key: SelectionKey) {
        val serverSocketChannel: ServerSocketChannel = key.channel() as ServerSocketChannel
        val clientSocketChannel: SocketChannel = serverSocketChannel.accept()
        clientSocketChannel.configureBlocking(false)
        val clientKey = clientSocketChannel.register(selector, SelectionKey.OP_READ)
        clientKey.attach(ConnectionCtx(clientKey, ConnectionType.FOR_NORMAL))
    }

    private fun handleRead(key: SelectionKey) {
        val clientSocketChannel = key.channel() as SocketChannel
        val connection = key.attachment() as ConnectionCtx
        if (!readFromOsToBuffer(clientSocketChannel, key, connection)) {
            return
        }
        when (connection.connectionType) {
            ConnectionType.FOR_NORMAL, ConnectionType.FOR_MASTER -> {
                for (req in respReader.parseRequests(connection)) {
                    for (commandHandler in commandHandlers) {
                        if (commandHandler.isHandle(req[0])) commandHandler.handle(connection, req)
                    }
                }
            }

            ConnectionType.FOR_FULLSYNC -> {

            }
        }
    }

    private fun readFromOsToBuffer(
        clientSocketChannel: SocketChannel,
        key: SelectionKey,
        connection: ConnectionCtx
    ): Boolean {
        var n: Int
        do {
            shareReadBuffer.clear()
            n = clientSocketChannel.read(shareReadBuffer)
            if (n > 0) {
                connection.feed(shareReadBuffer)
            }
        } while (n > 0) // 한 번 읽는 거로는 커널의 소켓 데이터를 모두 버퍼로 썼다는 보장 하지 못할 수 있다
        if (n == -1) {
            key.channel().close()
            key.cancel()
            return false
        }
        return true
    }

    private fun handleWrite(key: SelectionKey) {
        val clientSocketChannel = key.channel() as SocketChannel
        val connectionCtx = key.attachment() as ConnectionCtx

        while (connectionCtx.writeBufferQueue.isNotEmpty()) {
            val writerBuffer = connectionCtx.writeBufferQueue.first()
            clientSocketChannel.write(writerBuffer)
            if (writerBuffer.hasRemaining()) {
                return
            } else {
                connectionCtx.writeBufferQueue.removeFirst()
            }
        }
        key.interestOps(key.interestOps() and (SelectionKey.OP_WRITE.inv()))
    }
}
