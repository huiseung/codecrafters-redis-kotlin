import config.RedisConfig
import handler.Handler
import handler.command.BasicCommandHandler
import handler.command.ListCommandHandler
import handler.command.StringCommandHandler
import persistence.RdbManager
import protocol.CommandReader
import protocol.CommandResultWriter
import storage.StorageService
import java.net.ServerSocket
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!")
    val redisConfig = RedisConfig()
    redisConfig.initConfig(args)

    // Uncomment this block to pass the first stage
    val serverSocket = ServerSocket(redisConfig.get("port")!!.toInt())
    //
    // // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // // ensures that we don't run into 'Address already in use' errors


    val storageService = StorageService()
    val rdbManager = RdbManager(redisConfig, storageService)
    rdbManager.loadFromDisk()

    val commandReader = CommandReader()
    val commandResultWriter = CommandResultWriter()
    val commandHandler = listOf(
        BasicCommandHandler(commandResultWriter, redisConfig, storageService),
        StringCommandHandler(storageService, commandResultWriter),
        ListCommandHandler(storageService, commandResultWriter),
    )

    serverSocket.use {
        serverSocket.reuseAddress = true
        while (true) {
            val clientSocket = serverSocket.accept()
            val handler = Handler(clientSocket, commandHandler, commandReader, commandResultWriter)
            thread {
                handler.handle()
            }
        }
    }
}

