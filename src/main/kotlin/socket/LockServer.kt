package socket

//import java.net.ServerSocket
//import java.net.Socket
//import java.net.UnixDomainSocketAddress
//import java.nio.file.Path
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//import kotlin.io.path.deleteIfExists
//
//internal class LockServer(
//  private val socketFile: Path,
//  private val executor: ExecutorService = defaultExecutorService,
//) {
//  /** Track active readers per socket */
//  private val activeReaderCount: MutableMap<Socket, Int> = mutableMapOf()
//  private var writer: Socket? = null
//
//  init {
//    // Ensure the socket file doesn't exist already
//    socketFile.deleteIfExists()
//  }
//
//  fun start() {
//    val address = UnixDomainSocketAddress.of(socketFile)
//    ServerSocket().use { serverSocket ->
//      serverSocket.bind(address)
//      println("Lock server started at $socketFile")
//
//      while (true) {
//        val clientSocket = serverSocket.accept()
//        executor.submit { handleClient(clientSocket) }
//      }
//    }
//  }
//
//  private fun handleClient(client: Socket) {
//    client.use {
//      val input = client.getInputStream().bufferedReader()
//      val output = client.getOutputStream().bufferedWriter()
//
//      loop@ while (true) {
//        val request = input.readLine() ?: break
//        val msg = Request.entries.firstOrNull { it.name == request }
//
//        val response: Response = when (msg) {
//          Request.RequestRead -> {
//            if (writer == null) {
//              activeReaderCount[client] = activeReaderCount.getOrDefault(client, 0) + 1
//              Response.ReadGranted
//            } else {
//              Response.ReadDenied
//            }
//          }
//
//          Request.RequestWrite -> {
//            if (writer == null && activeReaderCount.isEmpty()) {
//              writer = client
//              Response.WriteGranted
//            } else {
//              Response.WriteDenied
//            }
//          }
//
//          Request.Release -> {
//            if (client == writer) {
//              writer = null
//            } else {
//              activeReaderCount[client] = activeReaderCount.getOrDefault(client, 0) - 1
//              if (activeReaderCount[client]!! <= 0) {
//                activeReaderCount.remove(client)
//              }
//            }
//            Response.Released
//          }
//
//          else                  -> {
//            Response.UnknownCommand
//          }
//        }
//
//        output.write("${response}\n")
//        output.flush()
//      }
//    }
//  }
//
//  companion object {
//    private val defaultExecutorService: ExecutorService = Executors.newCachedThreadPool()
//  }
//}
