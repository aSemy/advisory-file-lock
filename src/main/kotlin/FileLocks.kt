import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.util.*

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import com.amazonaws.annotation.ThreadSafe;
//import com.amazonaws.services.s3.transfer.exception.FileLockException;
//import com.amazonaws.util.IOUtils;
/**
 * An internal utility used to provide both inter and intra JVM file locking.
 * This works well as long as this class is loaded with the same class loader in
 * a JVM. Otherwise, intra-JVM file locking is not guaranteed.
 *
 *
 * Per javadoc of [FileLock], "File locks are held on behalf of the entire
 * Java virtual machine. They are not suitable for controlling access to a file
 * by multiple threads within the same virtual machine."
 *
 *
 * Hence the need for this utility class.
 */
//@ThreadSafe
object FileLocks {

  // External file lock doesn't seem to work correctly on Windows,
  // so disabling for now (Ref: TT0047889941)
  private const val EXTERNAL_LOCK = false
  //    private static final Log log = LogFactory.getLog(FileLocks.class);
  private val lockedFiles: MutableMap<File, RandomAccessFile> = TreeMap()

  /**
   * Acquires an exclusive lock on the specified file, creating the file as
   * necessary. Caller of this method is responsible to call the
   * [.unlock] method to prevent release leakage.
   *
   * @return true if the locking is successful; false otherwise.
   *
   * @throws FileLockException if we failed to lock the file
   */
  fun lock(file: File): Boolean {
    synchronized(lockedFiles) {
      if (lockedFiles.containsKey(file)) return false // already locked
    }
    var lock: FileLock? = null
    var raf: RandomAccessFile? = null
    try {
      // Note if the file does not already exist then an attempt will be
      // made to create it because of the use of "rw".
      raf = RandomAccessFile(file, "rw")
      if (EXTERNAL_LOCK) lock = raf.channel.lock()
    } catch (e: Exception) {
//      IOUtils.closeQuietly(raf, log)
      raf?.close()
      throw e
//      throw FileLockException(e)
    }
    val locked: Boolean
    synchronized(lockedFiles) {
      val prev = lockedFiles.put(file, raf)
      if (prev == null) {
        locked = true
      } else {
        // race condition: some other thread got locked it before this
        locked = false
        lockedFiles.put(file, prev) // put it back
      }
    }
    if (locked) {
//      if (log.isDebugEnabled())
        println("Locked file $file with $lock")
    } else {
      raf.close()
//      IOUtils.closeQuietly(raf, log)
    }
    return locked
  }

  /**
   * Returns true if the specified file is currently locked; false otherwise.
   */
  fun isFileLocked(file: File): Boolean {
    synchronized(lockedFiles) {
      return lockedFiles.containsKey(file)
    }
  }

  /**
   * Unlocks a file previously locked via [.lock].
   *
   * @return true if the unlock is successful; false otherwise. Successful
   * unlock means we have found and attempted to close the locking
   * file channel, but ignoring the fact that the close operation may
   * have actually failed.
   */
  fun unlock(file: File): Boolean {
    synchronized(lockedFiles) {
      val raf = lockedFiles[file]
      if (raf == null) return false
      else {
        // Must close out the channel before removing it from the map;
        // or else risk giving a false negative (of no lock but in fact
        // the file is still locked by the file system.)
        raf.close()
//        IOUtils.closeQuietly(raf, log)
        lockedFiles.remove(file)
      }
    }
//    if (log.isDebugEnabled()) log.debug("Unlocked file $file")
    return true
  }
}
