package niohandler

import network.ConnectionCtx
import network.ConnectionType
import protocol.Request
import protocol.RespWriter
import replica.ReplicaService
import storage.StorageService

class StringCommandHandler(
    private val storageService: StorageService,
    private val respWriter: RespWriter,
    private val replicaService: ReplicaService,
) : CommandHandler {
    private val cmds = setOf("SET", "GET")

    override fun isHandle(cmd: String): Boolean {
        return cmds.contains(cmd)
    }

    override fun handle(connectionCtx: ConnectionCtx, request: Request) {
        val cmd = request.request[0]
        when (cmd) {
            "SET" -> set(connectionCtx, request)
            "GET" -> get(connectionCtx, request)
        }
    }

    private fun set(connectionCtx: ConnectionCtx, request: Request) {
        val args = request.request.drop(1)
        val key = args[0]
        val value = args[1]
        if (args.size == 4 && args[2].uppercase() == "PX") {
            storageService.setString(key, value, args[3].toLong() + System.currentTimeMillis())
        } else {
            storageService.setString(key, value, null)
        }
        if (connectionCtx.connectionType == ConnectionType.FOR_MASTER) {
            connectionCtx.plusOffset(request.bytes)
            return
        }
        connectionCtx.writeBuffer(respWriter.writeSimpleString("OK"))
        replicaService.propagation(request)
    }

    private fun get(connectionCtx: ConnectionCtx, request: Request) {
        val args = request.request.drop(1)
        val key = args[0]
        val value = storageService.getString(key)
        if (value != null) {
            connectionCtx.writeBuffer(respWriter.writeBulkString(value))
            return
        }
        connectionCtx.writeBuffer(respWriter.writeNil())
    }
}
