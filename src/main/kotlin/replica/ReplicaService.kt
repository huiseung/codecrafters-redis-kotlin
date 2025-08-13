package replica

import config.RedisConfig
import protocol.RespWriter
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

class ReplicaService(
    private val config: RedisConfig,
    private val respWriter: RespWriter,
) {
    fun run() {
        if (config.get("role") != "slave") {
            return
        }
        if (config.get("master_host") == null || config.get("master_port") == null) {
            return
        }
        val masterChannel = connectToMaster()
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
    }
}
