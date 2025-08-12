import config.RedisConfig
import eventloop.MainEventLoop
import niohandler.BasicCommandHandler
import niohandler.ListCommandHandler
import niohandler.ReplicaCommandHandler
import niohandler.StringCommandHandler
import persistence.RdbManager
import storage.StorageService
import storage.WaiterService

fun main(args: Array<String>) {
    val redisConfig = RedisConfig()
    redisConfig.initConfig(args)

    val storageService = StorageService()
    val rdbManager = RdbManager(redisConfig, storageService)
    rdbManager.loadFromDisk()

    val waiterService = WaiterService()

    val commandHandlers = listOf(
        BasicCommandHandler(redisConfig, storageService),
        StringCommandHandler(storageService),
        ListCommandHandler(storageService, waiterService),
        ReplicaCommandHandler(redisConfig)
    )

    val mainEventLoop = MainEventLoop(redisConfig, commandHandlers, waiterService)
    mainEventLoop.init()

    mainEventLoop.run()
}

