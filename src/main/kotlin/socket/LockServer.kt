package socket

import java.net.ServerSocket
import java.net.Socket
import java.net.UnixDomainSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.deleteIfExists

class LockServer(private val socketFile: Path) {
  private val executor = Executors.newCachedThreadPool()
  /** Track active readers per socket */
  private val activeReaderCount = mutableMapOf<Socket, Int>()
  private var writer: Socket? = null

  init {
    // Ensure the socket file doesn't exist already
    socketFile.deleteIfExists()
  }

  fun start() {
    val address = UnixDomainSocketAddress.of(socketFile)
    ServerSocket().use { serverSocket ->
      serverSocket.bind(address)
      println("Lock server started at $socketFile")

      while (true) {
        val clientSocket = serverSocket.accept()
        executor.submit { handleClient(clientSocket) }
      }
    }
  }

  private fun handleClient(client: Socket) {
    client.use {
      val input = client.getInputStream().bufferedReader()
      val output = client.getOutputStream().bufferedWriter()

      loop@ while (true) {
        val request = input.readLine() ?: break
        val msg = Request.entries.firstOrNull { it.name == request }

        val response: Response = when (msg) {
          Request.REQUEST_READ  -> {
            if (writer == null) {
              activeReaderCount[client] = activeReaderCount.getOrDefault(client, 0) + 1
              Response.READ_GRANTED
            } else {
              Response.READ_DENIED
            }
          }

          Request.REQUEST_WRITE -> {
            if (writer == null && activeReaderCount.isEmpty()) {
              writer = client
              Response.WRITE_GRANTED
            } else {
              Response.WRITE_DENIED
            }
          }

          Request.RELEASE       -> {
            if (client == writer) {
              writer = null
            } else {
              activeReaderCount[client] = activeReaderCount.getOrDefault(client, 0) - 1
              if (activeReaderCount[client]!! <= 0) {
                activeReaderCount.remove(client)
              }
            }
            Response.RELEASED
          }

          else                  -> {
            Response.UNKNOWN_COMMAND
          }
        }

        output.write("${response}\n")
        output.flush()
      }
    }
  }
}
