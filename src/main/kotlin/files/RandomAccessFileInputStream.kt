package files

import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * Reads from a [RandomAccessFile]. Each operation reads from and advances the current position of the file.
 *
 *
 * Closing this stream does not close the underlying file.
 */
// TODO Replace this with Channels.newInputStream(SeekableByteChannel) or PositionTrackingFileChannelInputStream
class RandomAccessFileInputStream(private val file: RandomAccessFile) : InputStream() {
  @Throws(IOException::class)
  override fun skip(n: Long): Long {
    file.seek(file.filePointer + n)
    return n
  }

  @Throws(IOException::class)
  override fun read(bytes: ByteArray): Int {
    return file.read(bytes)
  }

  @Throws(IOException::class)
  override fun read(): Int {
    return file.read()
  }

  @Throws(IOException::class)
  override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
    return file.read(bytes, offset, length)
  }
}
