package demo

import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import serialization.kxsBinary


internal class FileReadWriteLock(
  lockFile: Path,
  private val socketDir: Path = Path(System.getProperty("java.io.tmpdir")).resolve("frwl"),
) : AutoCloseable {

  init {
    socketDir.createDirectories()
  }

  private val accessFile: RandomAccessFile =
    RandomAccessFile(lockFile.toFile(), "rw")

//  fun isOpen(): Boolean = accessFile.channel.isOpen

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
