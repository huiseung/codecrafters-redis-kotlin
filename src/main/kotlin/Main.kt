import handler.Handler
import handler.command.BasicCommandHandler
import handler.command.ListCommandHandler
import handler.command.StringCommandHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import protocol.CommandReader
import protocol.CommandResultWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!")

    // Uncomment this block to pass the first stage
    val serverSocket = ServerSocket(6379)
    //
    // // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // // ensures that we don't run into 'Address already in use' errors
    val config = RedisConfig()
    config.initConfig(args)

    val storageService = StorageService()

    val commandReader = CommandReader()
    val commandResultWriter = CommandResultWriter()
    val commandHandler = listOf(
        BasicCommandHandler(commandResultWriter, config),
        StringCommandHandler(storageService, commandResultWriter),
        ListCommandHandler(storageService, commandResultWriter),
    )



    serverSocket.use {
        serverSocket.reuseAddress = true
        while (true) {
            val clientSocket = serverSocket.accept()
            val handler = Handler(clientSocket, commandHandler, commandReader, commandResultWriter)
            CoroutineScope(Dispatchers.IO).launch {
                handler.handle()
            }
        }
    }
}

