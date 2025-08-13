import config.RedisConfig
import eventloop.MainEventLoop
import niohandler.BasicCommandHandler
import niohandler.ListCommandHandler
import niohandler.ReplicaCommandHandler
import niohandler.StringCommandHandler
import persistence.RdbManager
import protocol.RespWriter
import replica.ReplicaService
import storage.StorageService
import storage.WaiterService

fun main(args: Array<String>) {
    val redisConfig = RedisConfig()
    redisConfig.initConfig(args)

    val storageService = StorageService()
    val rdbManager = RdbManager(redisConfig, storageService)
    rdbManager.loadFromDisk()
    val respWriter = RespWriter()

    val waiterService = WaiterService(respWriter)

    val commandHandlers = listOf(
        BasicCommandHandler(redisConfig, storageService, respWriter),
        StringCommandHandler(storageService, respWriter),
        ListCommandHandler(storageService, waiterService, respWriter),
        ReplicaCommandHandler(redisConfig, respWriter)
    )

    val replicaService = ReplicaService(redisConfig, respWriter)
    replicaService.run()

    val mainEventLoop = MainEventLoop(redisConfig, commandHandlers, waiterService, replicaService)
    mainEventLoop.init()

    mainEventLoop.run()
}

