package files

import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile

/**
 * Writes to a [RandomAccessFile]. Each operation writes to and advances the current position of the file.
 *
 *
 * Closing this stream does not close the underlying file. Flushing this stream does nothing.
 */
// TODO Replace with Channels.newOutputStream(SeekableByteChannel)
class RandomAccessFileOutputStream(private val file: RandomAccessFile) : OutputStream() {
  @Throws(IOException::class)
  override fun write(i: Int) {
    file.write(i)
  }

  @Throws(IOException::class)
  override fun write(bytes: ByteArray) {
    file.write(bytes)
  }

  @Throws(IOException::class)
  override fun write(bytes: ByteArray, offset: Int, length: Int) {
    file.write(bytes, offset, length)
  }
}
