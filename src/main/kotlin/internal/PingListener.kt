package dev.adamko.advisoryfilelock.internal

import dev.adamko.advisoryfilelock.ReadLock
import java.lang.ref.WeakReference
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ServerSocketChannel
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

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
    println("[reader $id] Finished run!")
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

    println("[reader $id] Listening for ping on $socketAddress")
    serverSocket.use { socket ->
      while (!interrupted()) {
        if (lockRef.get() == null) {
          println("[reader $id] lockRef expired, closing.")
          break
        }

        val conn = try {
          socket.accept() ?: continue
        } catch (ex: ClosedByInterruptException) {
          println("[reader $id] listener closed $ex")
          continue
        }
//        println("[reader $id] Received connection from $conn")
        conn.use { client ->
          val buf = ByteBuffer.allocate(Int.SIZE_BYTES)

          val readCount = client.read(buf)
          if (readCount <= 0) {
            println("[reader $id] connection closed")
          } else {
            val message = buf.flip().getInt()
            val response = when (message) {
              1    -> 1
              else -> -1
            }
            println("[reader $id] Handling request $message, responding with $response")
            client.write(buf.clear().putInt(response))
          }
        }
        threadSleep(100.milliseconds)
      }
      println("[reader $id] Finished thread!")
    }
  }

//  private fun net() {
////    val socketAddress: UnixDomainSocketAddress =
////      UnixDomainSocketAddress.of(socketFile)
//
////    val serverSocket: ServerSocketChannel =
////      ServerSocketChannel.open(StandardProtocolFamily.UNIX)
////        .bind(socketAddress)
////        .apply {
////          configureBlocking(false)
////        }
//    val socketAddress = InetSocketAddress(0).address
//    val serverSocket = DatagramSocket(0, socketAddress)
//    serverSocket.soTimeout = 1.seconds.inWholeMilliseconds.toInt()
//
//    println("[reader $id] Listening for ping on $socketAddress")
//    serverSocket.use { socket ->
//      while (!interrupted()) {
//        if (lockRef.get() == null) {
//          println("[reader $id] lockRef expired, closing.")
//          break
//        }
//
//        val receiveBytes = ByteArray(Byte.SIZE_BYTES)
//        while (true) {
//          val getack = DatagramPacket(receiveBytes, receiveBytes.size)
//          try {
//            socket.receive(getack)
//          } catch (e: SocketTimeoutException) {
//            continue
//          }
//        }
//
//
//        val conn = try {
//          socket.accept() ?: continue
//        } catch (ex: ClosedByInterruptException) {
//          println("[reader $id] listener closed $ex")
//          continue
//        }
////        println("[reader $id] Received connection from $conn")
//        conn.use { client ->
//          val buf = ByteBuffer.allocate(Int.SIZE_BYTES)
//
//          val readCount = client.read(buf)
//          if (readCount <= 0) {
//            println("[reader $id] connection closed")
//          } else {
//            val message = buf.flip().getInt()
//            val response = when (message) {
//              1    -> 1
//              else -> -1
//            }
//            println("[reader $id] Handling request $message, responding with $response")
//            val bytesToSend = byteArrayOf(response.toByte())
//            socket.send(DatagramPacket(bytesToSend, bytesToSend.size, address, ownerPort))
//          }
//        }
//        threadSleep(100.milliseconds)
//      }
//
//      println("[reader $id] Finished thread!")
//    }
//    println("[reader $id] Finished run!")
//  }
}
