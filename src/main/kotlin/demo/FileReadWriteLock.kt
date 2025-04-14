package demo

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import serialization.kxsBinary


internal class FileReadWriteLock(
  private val lockFile: Path,
  private val socketDir: Path = Path(System.getProperty("java.io.tmpdir")).resolve("frwl"),
) : AutoCloseable {

  init {
    socketDir.createDirectories()
  }

  private val accessFile: RandomAccessFile =
    RandomAccessFile(lockFile.toFile(), "rw")

  fun isOpen(): Boolean = accessFile.channel.isOpen

  fun readLock(): LockAccess {
    return ReadLock(
      channel = accessFile.channel,
      socketDir = socketDir,
    )
  }

  fun writeLock(): LockAccess {
    return WriteLock(accessFile.channel)
  }

  override fun close() {
    accessFile.close()
  }
}

internal sealed interface LockAccess : AutoCloseable {
  fun lock()
  fun unlock()
  override fun close(): Unit = unlock()
}

private fun randomAlphaNumericString(size: Int = 12): String {
  val chars: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
  return buildString {
    repeat(size) {
      append(chars.random())
    }
  }
}

/**
 * Manage read lock on a file.
 *
 * Can be locked/unlocked multiple times.
 */
private class ReadLock(
  private val channel: FileChannel,
  private val socketDir: Path,
  private val id: String = randomAlphaNumericString(),
) : LockAccess {

  //    if (!socketPath.exists()) {
//      socketPath.createFile()
//    }
  private val socketPath: Path = socketDir.resolve(id)
    .absolute()
//    .toRealPath()

  init {
    PingListener(
      id = id,
      lockRef = WeakReference(this),
      socketPath = socketPath,
    )

    // TODO delete socketPath on exit.
    //      Currently I'm testing and it's nice to have a bad implementation to see what happens when the sockets aren't removed.
  }

  override fun lock() {
    channel.lockLenient().use { _ ->
      println("[reader $id] Locking")
      val data = channel.readLockFileData()
      val newData = data.addReader(socketPath)
      channel.writeLockFileData(newData)
    }
  }

  override fun unlock() {
    channel.lockLenient().use { _ ->
      println("[reader $id] Unlocking")
//      pingListener?.interrupt()
//      pingListener = null
      val data = channel.readLockFileData()
      if (socketPath !in data.readers) {
        System.err.println("[reader $id] lock file data does not contain $socketPath")
      } else {
        val newData = data.removeReader(socketPath)
        channel.writeLockFileData(newData)
      }
    }
  }

  override fun close() {
    unlock()
  }

//  private fun createSocket() {
//    if (!socketPath.exists()) {
//      println("[reader $id] Creating socket at $socketPath")
//      socketPath.createFile()
//      serverSocket = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
//        .bind(null) // use 'null' to automatically assign a socket address
//        .apply {
//          configureBlocking(true)
//        }
//
//      listenForPing()
//    }
//  }

//  private fun cleanupSocket() {
//    serverSocket?.close()
//    serverSocket = null
//    socketPath.deleteIfExists()
//  }

//  private fun listenForPing(): Thread {
//    return thread(isDaemon = true, name = "ReadLock-listener-${id}") {
//      println("[reader $id] Listening for ping from $socketPath")
//      try {
//        serverSocket.use { socket ->
//          while (!Thread.interrupted()) {
//            val conn = try {
//              socket.accept() ?: continue
//            } catch (ex: ClosedByInterruptException) {
//              println("[reader $id] listener closed by interrupt")
//              break
//            }
//            println("[reader $id] Received connection from $conn")
//            conn.use { client ->
//              val buf = ByteBuffer.allocate(Int.SIZE_BYTES)
//              client.read(buf)
//              val message = buf.flip().getInt()
//              val response = when (message) {
//                1    -> 1
//                else -> -1
//              }
//              println("[reader $id] Handling request $message, responding with $response")
//              client.write(buf.clear().putInt(response))
//            }
////          threadSleep(100.milliseconds)
//          }
//        }
//      } finally {
////        val updatedData = channel.readLockFileData().removeReader(socketPath)
////        channel.writeLockFileData(updatedData)
////        println("[reader $id] closed listener $socketPath")
//      }
//    }
//  }
}


private class PingListener(
  private val id: String,
  private val lockRef: WeakReference<ReadLock>,
  private val socketPath: Path,
) : Thread(
  "ReadLock-listener-$id"
) {
  init {
    isDaemon = true

    start()
  }

  override fun run() {
    val socketAddress: UnixDomainSocketAddress =
      UnixDomainSocketAddress.of(socketPath)

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
    println("[reader $id] Finished run!")
  }
}


private class WriteLock(
  private val channel: FileChannel
) : LockAccess {
  private var lock: FileLock? = null

  override fun lock() {
    this.lock = acquireLock()
  }

  private tailrec fun acquireLock(): FileLock {
    val lock: FileLock = channel.lockLenient()
    try {
      val data = channel.readLockFileData()
      if (data.readers.isEmpty()) {
        return lock
      } else {
        println("[writer] Waiting for ${data.readers.size} readers to finish")
        refreshReaders()
        lock.release()
        Thread.sleep(Random.nextLong(25, 125))
      }
    } catch (ex: Throwable) {
      lock.release()
      throw ex
    }
    return acquireLock()
  }

  override fun unlock() {
    lock?.release()
  }

  override fun close() {
    unlock()
  }

  private fun refreshReaders() {
    val storedData = channel.readLockFileData()
    val aliveReaders = storedData.readers.filterTo(sortedSetOf()) { isReaderAlive(it) }

    val actualData = LockFileData(aliveReaders)

    if (storedData != actualData) {
      val mismatch =
        (storedData.readers union actualData.readers) subtract (storedData.readers intersect actualData.readers)
      println("stored readers != alive readers\n\tmismatch:$mismatch\n\tstored:${storedData}\n\tactual:${actualData}")
      channel.writeLockFileData(actualData)
    }
  }

  private fun isReaderAlive(socketPath: Path): Boolean {
//    println("[writer] Checking if reader at $socketPath is alive...")
    if (!socketPath.exists()) return false

    var attempts = 0
    val maxAttempts = 5
    while (++attempts < maxAttempts) {

      try {
        val client = SocketChannel.open(UnixDomainSocketAddress.of(socketPath))
        client.configureBlocking(false)
        client.finishConnect()
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
          .putInt(1)
          .flip()
        while (buffer.hasRemaining()) {
          client.write(buffer)
        }
        buffer.clear()
        client.read(buffer)
        val response = buffer.getInt()

        val isAlive = response == 1
        if (!isAlive) {
          println("[writer] Reader $socketPath is dead")
        }
        return isAlive
      } catch (ex: IOException) {
        System.err.println("[writer] failed to check reader $socketPath: $ex")
        //return false
        threadSleep(500.milliseconds)
        continue
      }
    }
    System.err.println("[writer] failed to check reader $socketPath after $attempts attempts")

    return false
  }
}

//private fun isReaderAlive2(socketPath: Path): Boolean {
//  println("Checking if reader at $socketPath is alive...")
//  if (!socketPath.exists()) return false // Socket file should exist
//
//
//  try {
//    // Open socket channel
//    SocketChannel.open().use { client ->
//      val address = UnixDomainSocketAddress.of(socketPath)
//      client.connect(address)
//
//      if (!client.isConnected) {
//        println("Failed to connect to $socketPath")
//        return false
//      }
//
//      // Send a "ping" message
//      val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
//      buffer.putInt(1).flip()
//      client.write(buffer)
//
//      // Wait for a response within the timeout period
//      buffer.clear()
//      // Start countdown for operation timeout
//      val timeoutMark = TimeSource.Monotonic.markNow() + 5.seconds
//      while (timeoutMark.hasNotPassedNow()) {
//        val bytesRead = client.read(buffer)
//        if (bytesRead > 0) {
//          buffer.flip()
//          val response = buffer.getInt()
//          println("[writer] Pinged $socketPath, got response $response")
//          return response == 1
//        }
//        // Sleep briefly to avoid busy-waiting
//        threadSleep(100.milliseconds)
//      }
//    }
//  } catch (ex: IOException) {
//    println("[writer] Communication with $socketPath failed: ${ex.message}")
//    return false // Treat I/O errors as "reader not alive"
//  }
//
//  return false
//  // If the countdown expires, consider it a timeout
////  throw LockTimeoutException("Timed out while checking if reader is alive", socketPath.toFile())
//}

@Serializable
private data class LockFileData(
  val readers: @Serializable(SortedSetSerializer::class) SortedSet<@Serializable(PathAsStringSerializer::class) Path>,
) {
  fun addReader(reader: Path): LockFileData =
    copy(readers = (readers union setOf(reader)).toSortedSet())

  fun removeReader(reader: Path): LockFileData =
    copy(readers = (readers - setOf(reader)).toSortedSet())

//  companion object {
//    val descriptorChecksum: String by lazy {
//      computeChecksum(serializer().descriptor)
//    }
//  }
}

internal class SortedSetSerializer<T : Comparable<T>>(
  valueSerializer: KSerializer<T>,
) : KSerializer<SortedSet<T>> {
  private val delegate = SetSerializer(valueSerializer)
  override val descriptor get() = delegate.descriptor
  override fun deserialize(decoder: Decoder): SortedSet<T> {
    return delegate.deserialize(decoder).toSortedSet()
  }

  override fun serialize(encoder: Encoder, value: SortedSet<T>) {
    encoder.encodeSerializableValue(delegate, value)
  }
}

internal class PathAsStringSerializer : KSerializer<Path> {
  override val descriptor = String.serializer().descriptor
  override fun deserialize(decoder: Decoder): Path {
    return Path(decoder.decodeString())
  }

  override fun serialize(encoder: Encoder, value: Path) {
    encoder.encodeString(value.absolute().normalize().invariantSeparatorsPathString)
  }
}

private fun FileChannel.writeLockFileData(data: LockFileData) {
  val encoded = kxsBinary.encodeToByteArray(LockFileData.serializer(), data)
  write(ByteBuffer.wrap(encoded), 0)
}

private fun FileChannel.readLockFileData(): LockFileData {
  if (size() == 0L) {
    println("Lock file is empty, returning empty data")
    return LockFileData(sortedSetOf())
  }
  position(0)
  val bytes = ByteArrayOutputStream().use { os ->
    val buf = ByteBuffer.allocate(1024)
    while (read(buf) > 0) {
      os.write(buf.array(), 0, buf.limit())
      buf.clear()
    }
    os.toByteArray()
  }

  val data = kxsBinary.decodeFromByteArray(LockFileData.serializer(), bytes)
//  println("read lock file data: $data")
  return data
}


//private fun computeChecksum(descriptor: SerialDescriptor): String {
//  val messageDigest = MessageDigest.getInstance("SHA-256")
//
//  DigestOutputStream(nullOutputStream(), messageDigest).use { digestStream ->
//    fun hash(value: String): Unit =
//      digestStream.write(value.toByteArray())
//
//    fun hash(value: Boolean): Unit =
//      digestStream.write(if (value) 1 else 0)
//
//    fun hash(value: Int) {
//      digestStream.write(value)
//    }
//
//    tailrec fun updateDigestWithDescriptor(
//      queue: MutableList<SerialDescriptor> = mutableListOf(descriptor),
//      visited: MutableSet<SerialDescriptor> = mutableSetOf(),
//    ) {
//      val desc = queue.removeFirstOrNull() ?: return
//
//      // Update with the name and kind of the descriptor
//      hash(desc.serialName)
//      hash(desc.kind.hashCode())
//      hash(desc.isNullable)
//      hash(desc.isInline)
//      desc.annotations.forEach { annotation ->
//        hash(annotation.hashCode())
//      }
//
//      hash(desc.elementsCount)
//      repeat(desc.elementsCount) { index ->
//        hash(desc.getElementName(index))
//        hash(desc.isElementOptional(index))
//        desc.getElementAnnotations(index).forEach { annotation ->
//          hash(annotation.hashCode())
//        }
//
//        val element = desc.getElementDescriptor(index)
//        if (visited.add(element)) {
//          queue.add(element)
//        }
//      }
//
//      updateDigestWithDescriptor(queue, visited)
//    }
//
//    updateDigestWithDescriptor()
//  }
//
//  return Base64.getEncoder().encodeToString(messageDigest.digest())
//}
