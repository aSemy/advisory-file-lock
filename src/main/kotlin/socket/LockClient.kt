package socket

//import java.net.Socket
//import java.net.UnixDomainSocketAddress
//import java.nio.file.Path
//
//internal class LockClient(private val socketFile: Path) {
//  private lateinit var socket: Socket
//
//  fun connect() {
//    val address = UnixDomainSocketAddress.of(socketFile)
//    socket = Socket()
//    socket.connect(address)
//    println("Connected to lock server at $socketFile")
//  }
//
//  fun requestReadLock(): Boolean {
//    sendMessage(Request.RequestRead)
//    return readResponse() == Response.ReadGranted
//  }
//
//  fun requestWriteLock(): Boolean {
//    sendMessage(Request.RequestWrite)
//    return readResponse() == Response.WriteGranted
//  }
//
//  fun releaseLock(): Boolean {
//    sendMessage(Request.Release)
//    return readResponse() == Response.Released
//  }
//
//  private fun sendMessage(message: Request) {
//    val output = socket.getOutputStream().bufferedWriter()
//    output.write("$message\n")
//    output.flush()
//  }
//
//  private fun readResponse(): Response? {
//    val input = socket.getInputStream().bufferedReader()
//    val line = input.readLine()
//    return Response.entries.firstOrNull { it.name == line }
////    return input.readLine() ?: "UNKNOWN_RESPONSE"
//  }
//
//  fun close() {
//    socket.close()
//    println("Disconnected from lock server!")
//  }
//}
