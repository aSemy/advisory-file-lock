package dev.adamko.advisoryfilelock.internal

import dev.adamko.advisoryfilelock.ReadLock
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ServerSocketChannel
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class PingListener(
  private val id: String,
  private val lockRef: WeakReference<ReadLock>,
  private val socketFile: Path,
) : Thread("ReadLock-listener") {
  init {
    isDaemon = true

    start()
  }

  override fun run() {
    uds()
//    net()
    logger.fine("[reader $id] Finished run!")
  }

  private fun uds() {
    val socketAddress: UnixDomainSocketAddress =
      UnixDomainSocketAddress.of(socketFile)

    val serverSocket: ServerSocketChannel =
      ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        .bind(socketAddress)
        .apply {
          configureBlocking(false)
        }

    logger.fine("[reader $id] Listening for ping on $socketAddress")
    serverSocket.use { socket ->
      while (!interrupted()) {
        if (lockRef.get() == null) {
          logger.fine("[reader $id] lockRef expired, closing.")
          break
        }

        val conn = try {
          socket.accept() ?: continue
        } catch (ex: ClosedByInterruptException) {
          logger.fine("[reader $id] listener closed $ex")
          continue
        }
//        logger.fine("[reader $id] Received connection from $conn")
        conn.use { client ->
          val buf = ByteBuffer.allocate(Int.SIZE_BYTES)

          val readCount = client.read(buf)
          if (readCount <= 0) {
            logger.fine("[reader $id] connection closed")
          } else {
            val message = buf.flip().getInt()
            val response = when (message) {
              1    -> 1
              else -> -1
            }
            logger.fine("[reader $id] Handling request $message, responding with $response")
            client.write(buf.clear().putInt(response))
          }
        }
        threadSleep(100.milliseconds)
      }
      logger.fine("[reader $id] Finished thread!")
    }
  }

  private fun net() {
    val serverSocket = ServerSocket(0).apply {
      soTimeout = 1.seconds.inWholeMilliseconds.toInt()
    }
    val port = serverSocket.localPort

    logger.fine("[reader $id] Listening for ping on port $port")
    serverSocket.use { socket ->
      while (!interrupted()) {
        if (lockRef.get() == null) {
          logger.fine("[reader $id] lockRef expired, closing.")
          break
        }

        val conn = try {
          socket.accept()
        } catch (ex: SocketTimeoutException) {
          // Connection timeout, continue looping
          continue
        } catch (ex: IOException) {
          logger.fine("[reader $id] Error while accepting connection: ${ex.message}")
          break
        }

        conn.use { client ->
          val input = client.getInputStream()
          val output = client.getOutputStream()

          val buffer = ByteArray(Int.SIZE_BYTES)
          val readCount = input.read(buffer)
          if (readCount <= 0) {
            logger.fine("[reader $id] Client disconnected.")
          } else {
            val message = ByteBuffer.wrap(buffer).int
            val response = when (message) {
              1    -> 1
              else -> -1
            }
            logger.fine("[reader $id] Handling request $message, responding with $response")
            output.write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(response).array())
          }
        }
      }
      logger.fine("[reader $id] Finished thread!")
    }
  }

  companion object {
    private val logger: Logger = Logger.getLogger(PingListener::class.qualifiedName)
  }
}
