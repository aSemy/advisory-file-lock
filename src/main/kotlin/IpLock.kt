import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createFile

/**
 * An inter-process lock for synchronization of multiple JVM based processes running on the same machine.
 *
 *
 * If the process that owns the lock finishes without releasing it, the lock is released automatically. This is also
 * valid if the process owning the lock is destroyed or killed.
 *
 * The synchronization is implemented based on [FileLock].
 *
 * This class is thread-safe: multiple threads can share a single
 * [IpLock] object without the need for external synchronization.
 *
 * @author Andreas Klöber
 * @see java.nio.channels.FileLock
 */
class IpLock
/**
 * Create a new lock object that uses the given file for synchronization. The file will be created if it does not
 * exist.
 *
 * @param syncFile the file to be used for synchronization
 */(/*
     * The synchronization file.
     */private val syncFile: Path
) : AutoCloseable {
  /**
   * The underlying [FileLock] object.
   */
  private var lock: FileLock? = null

//  /**
//   * Create a new lock object that uses the given file for synchronization. The file will be created if it does not
//   * exist.
//   *
//   * @param syncFilePath path to the file to be used for synchronization
//   */
//  constructor(syncFilePath: String) : this(Path(syncFilePath))

  /**
   * Acquires the lock in a blocking way.
   *
   * Only one process can acquire the lock at the same time. This method waits indefinitely until the lock could be
   * acquired.
   */
  fun lock() {
    synchronized(this) {
      this.lock = createSyncChannel(this.syncFile).lock()
    }
  }

  /**
   * Acquires the lock in a blocking way.
   *
   *
   * Only one process can acquire the lock at the same time. In addition to [.lock] this method also allows
   * configuration of a timeout.
   *
   *
   * As the underlying [FileLock] object does not provide a way to cancel a lock request in case of a timeout,
   * this method periodically tries to get the lock until this is successful or the timeout limit is reached.
   *
   * @param timeout         the timeout limit
   * @param tryLockInterval the time interval for trying locks
   * @param timeUnit        the [TimeUnit] for both <tt>timeout</tt> and <tt>tryLockInterval</tt> parameters
   * @return `true` if the lock could be required; `false` if there was a timeout
   * @throws IOException          if the synchronization file could not be created (e.g. because of missing write permissions
   * in target folder) or if some other I/O error occurs on the underlying [FileLock]
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Throws(IOException::class, InterruptedException::class)
  fun lock(timeout: Long, tryLockInterval: Long, timeUnit: TimeUnit): Boolean {
    synchronized(this) {
      // schedule task for lock timeout
      val timeoutSignal = CountDownLatch(1)
      val lockTimeoutTimer = Timer(true)
      val lockTimeoutTask: TimerTask = object : TimerTask() {
        override fun run() {
          timeoutSignal.countDown()
        }
      }
      try {
        lockTimeoutTimer.schedule(lockTimeoutTask, timeUnit.toMillis(timeout))

        while (this.lock == null) {
          this.lock = createSyncChannel(this.syncFile).tryLock()
          if (this.lock == null) {
            // wait interval before next tryLock()
            if (timeoutSignal.await(tryLockInterval, timeUnit)) {
              // timeout signaled
              return false
            }
          }
        }

        return true
      } finally {
        // unschedule task for lock timeout detection
        lockTimeoutTask.cancel()
        lockTimeoutTimer.cancel()
      }
    }
  }

  /**
   * Tries to acquire the lock and returns immediately.
   *
   * Only one process can acquire the lock at the same time. The result determines whether the lock could be acquired
   * or not.
   *
   * @return `true` if the lock could be required; `false` if there was a timeout
   * @throws IOException if the synchronization file could not be created (e.g. because of missing write permissions
   * in target folder) or if some other I/O error occurs on the underlying [FileLock]
   */
  @Throws(IOException::class)
  fun tryLock(): Boolean {
    synchronized(this) {
      this.lock = createSyncChannel(this.syncFile).tryLock()
      return this.lock != null
    }
  }

  /**
   * Releases the lock.
   *
   * If the lock has not been acquired before, this method returns immediately.
   *
   * @throws IOException if some other I/O error occurs on the underlying [FileLock]
   */
  @Throws(IOException::class)
  fun unlock() {
    synchronized(this) {
      if (this.lock == null) {
        // there is no lock
        return
      }
      lock!!.release()
    }
  }

  /**
   * {@inheritDoc}
   */
  @Throws(Exception::class)
  override fun close() {
    unlock()
  }

  companion object {
    /**
     * Creates a synchronization channel based on the given file. If the file does not exist yet, it is also created.
     *
     * @param file the underlying file
     * @return the created [FileChannel]
     */
    private fun createSyncChannel(file: Path): FileChannel {
      // make sure sync file exists
      file.createFile()
      val raFile = RandomAccessFile(file.toFile(), "rw")
      return raFile.channel
    }
  }
}
