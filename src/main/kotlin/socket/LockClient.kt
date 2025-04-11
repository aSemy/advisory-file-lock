package socket

import java.net.Socket
import java.net.UnixDomainSocketAddress
import java.nio.file.Path

class LockClient(private val socketFile: Path) {
  private lateinit var socket: Socket

  fun connect() {
    val address = UnixDomainSocketAddress.of(socketFile)
    socket = Socket()
    socket.connect(address)
    println("Connected to lock server at $socketFile")
  }

  fun requestReadLock(): Boolean {
    sendMessage(Request.REQUEST_READ )
    return readResponse() == Response.READ_GRANTED
  }

  fun requestWriteLock(): Boolean {
    sendMessage(Request.REQUEST_WRITE )
    return readResponse() == Response.WRITE_GRANTED
  }

  fun releaseLock(): Boolean {
    sendMessage(Request.RELEASE)
    return readResponse() == Response.RELEASED
  }

  private fun sendMessage(message: Request) {
    val output = socket.getOutputStream().bufferedWriter()
    output.write("$message\n")
    output.flush()
  }

  private fun readResponse(): Response? {
    val input = socket.getInputStream().bufferedReader()
    val line = input.readLine()
    return Response.entries.firstOrNull { it.name == line }
//    return input.readLine() ?: "UNKNOWN_RESPONSE"
  }

  fun close() {
    socket.close()
    println("Disconnected from lock server!")
  }
}
