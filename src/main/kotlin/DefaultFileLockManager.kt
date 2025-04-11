import filelock.*
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.function.LongSupplier
import java.util.function.Supplier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import process.ProcessMetaDataProvider
import time.ExponentialBackoff
import time.ExponentialBackoff.Result.Companion.notSuccessful
import time.ExponentialBackoff.Result.Companion.successful

//import com.google.common.annotations.VisibleForTesting;
//import org.gradle.cache.*;
//import org.gradle.cache.internal.filelock.*;
//import FileLockContentionHandler;
//import org.gradle.internal.concurrent.CompositeStoppable;
//import org.gradle.internal.concurrent.Stoppable;
//import org.gradle.internal.time.ExponentialBackoff;
//import org.jspecify.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import static org.gradle.internal.UncheckedException.throwAsUncheckedException;
/**
 * Uses file system locks on a lock file per target file.
 */
class DefaultFileLockManager internal constructor(
  private val metaDataProvider: ProcessMetaDataProvider,
  private val fileLockContentionHandler: FileLockContentionHandler,
  private val lockTimeout: Duration = DEFAULT_LOCK_TIMEOUT,
  private val generator: LongSupplier = LongSupplier { Random().nextLong() },
) : FileLockManager {
  private val lockedFiles: MutableSet<File> = CopyOnWriteArraySet()
  private val shortTimeout: Duration = 10000.milliseconds

//  constructor(
//    metaDataProvider: ProcessMetaDataProvider,
//    fileLockContentionHandler: FileLockContentionHandler,
//  ) : this(
//    metaDataProvider,
//    fileLockContentionHandler,
//    DEFAULT_LOCK_TIMEOUT,
//  )

//  constructor(
//    metaDataProvider: ProcessMetaDataProvider,
//    lockTimeout: Duration,
//    fileLockContentionHandler: FileLockContentionHandler
//  ) : this(metaDataProvider, lockTimeout, fileLockContentionHandler )

//  private class RandomLongIdGenerator : LongSupplier {
//    private val random = Random()
//
//    override fun getAsLong(): Long {
//      return random.nextLong()
//    }
//  }


  //    @Override
  //    public FileLock lock(File target, LockOptions options, String targetDisplayName) throws LockTimeoutException {
  //        return lock(target, options, targetDisplayName, "");
  //    }
  //
  //    @Override
  //    public FileLock lock(File target, LockOptions options, String targetDisplayName, String operationDisplayName) {
  //        return lock(target, options, targetDisplayName, operationDisplayName, null);
  //    }
  override fun lock(
    target: File,
    options: LockOptions,
    targetDisplayName: String,
    operationDisplayName: String,
    whenContended: Consumer<FileLockReleasedSignal>?
  ): FileLock {
    if (options.mode == FileLockManager.LockMode.OnDemand) {
      throw UnsupportedOperationException("No $options mode lock implementation available.")
    }
    val canonicalTarget: File
    try {
      canonicalTarget = target.canonicalFile
    } catch (e: IOException) {
      throw UncheckedIOException(e)
    }
    check(lockedFiles.add(canonicalTarget)) {
      String.format(
        "Cannot lock %s as it has already been locked by this process.",
        targetDisplayName
      )
    }
    try {
      val port = fileLockContentionHandler.reservePort()
      return DefaultFileLock(canonicalTarget, options, targetDisplayName, operationDisplayName, port, whenContended)
    } catch (t: Throwable) {
      lockedFiles.remove(canonicalTarget)
      throw RuntimeException(t)
    }
  }

  private inner class DefaultFileLock(
    target: File,
    options: LockOptions,
    displayName: String,
    operationDisplayName: String,
    private val port: Int,
    whenContended: Consumer<FileLockReleasedSignal>?
  ) : AbstractFileAccess(), FileLock {
    private val lockFile: File
    private val target: File
    override val mode: FileLockManager.LockMode
    private val displayName: String
    private val operationDisplayName: String
    private var lock: java.nio.channels.FileLock? = null
    private val lockFileAccess: LockFileAccess
    private var lockState: LockState? = null
    private val lockId = generator.asLong

    init {
      if (options.mode == FileLockManager.LockMode.OnDemand) {
        throw UnsupportedOperationException("Locking mode OnDemand is not supported.")
      }

      this.target = target

      this.displayName = displayName
      this.operationDisplayName = operationDisplayName
      this.lockFile = determineLockTargetFile(target)

      try {
        lockFile.parentFile.mkdirs()
        //                org.apache.commons.io.FileUtils.forceMkdirParent(lockFile);
        lockFile.createNewFile()
      } catch (e: IOException) {
        println("Couldn't create lock file for $lockFile")
        throw e
      }

      val stateProtocol =
        if (options.isUseCrossVersionImplementation) Version1LockStateSerializer() else DefaultLockStateSerializer()
      lockFileAccess = LockFileAccess(lockFile, LockStateAccess(stateProtocol))
      try {
        if (whenContended != null) {
          fileLockContentionHandler.start(lockId, whenContended)
        }
        lockState = lock(options.mode)
      } catch (t: Throwable) {
        // Also releases any locks
        lockFileAccess.close()
        throw t
      }

      this.mode = if (lock!!.isShared) FileLockManager.LockMode.Shared else FileLockManager.LockMode.Exclusive
    }

    override fun isLockFile(file: Path): Boolean {
      return file == lockFile
    }

    override val unlockedCleanly: Boolean
      get() {
        assertOpen()
        return !lockState!!.isDirty
      }

    override val state: FileLock.State?
      get() {
        assertOpen()
        return lockState
      }

    @Throws(LockTimeoutException::class, FileIntegrityViolationException::class)
    override fun <T> readFile(action: Supplier<out T>): T {
      assertOpenAndIntegral()
      return action.get()
    }

    @Throws(LockTimeoutException::class, FileIntegrityViolationException::class)
    override fun updateFile(action: Runnable) {
      assertOpenAndIntegral()
      doWriteAction(action)
    }

    @Throws(LockTimeoutException::class)
    override fun writeFile(action: Runnable) {
      assertOpen()
      doWriteAction(action)
    }

    fun doWriteAction(action: Runnable) {
      if (mode != FileLockManager.LockMode.Exclusive) {
        throw InsufficientLockModeException("An exclusive lock is required for this operation")
      }

      try {
        lockState = lockFileAccess.markDirty(lockState!!)
        action.run()
        lockState = lockFileAccess.markClean(lockState!!)
      } catch (t: Throwable) {
        throw RuntimeException(t)
      }
    }

    fun assertOpen() {
      checkNotNull(lock) { "This lock has been closed." }
    }

    fun assertOpenAndIntegral() {
      assertOpen()
      if (lockState!!.isDirty) {
        throw FileIntegrityViolationException(String.format("The file '%s' was not unlocked cleanly", target))
      }
    }

    override fun close() {
      val stoppables = ArrayDeque<Runnable>()
      //            CompositeStoppable stoppable = new CompositeStoppable();
      stoppables.add(Runnable {
        if (lockFileAccess.isClosed()) {
          return@Runnable
        }
        try {
          println(String.format("Releasing lock on %s.", displayName))
          try {
            if (lock != null && !lock!!.isShared) {
              // Discard information region
              val lockOutcome: FileLockOutcome
              try {
                lockOutcome =
                  lockInformationRegion(FileLockManager.LockMode.Exclusive, newExponentialBackoff())
              } catch (e: InterruptedException) {
                throw RuntimeException(e)
              }
              if (lockOutcome.isLockWasAcquired) {
                try {
                  lockFileAccess.clearLockInfo()
                } finally {
                  lockOutcome.fileLock.release()
                }
              }
            }
          } finally {
            lockFileAccess.close()
          }
        } catch (e: Exception) {
          throw RuntimeException("Failed to release lock on $displayName", e)
        }
      })
      stoppables.add(Runnable {
        try {
          fileLockContentionHandler.stop(lockId)
        } catch (e: Exception) {
          throw RuntimeException("Unable to stop listening for file lock requests for $displayName", e)
        }
      })
      stoppables.add(Runnable {
        lock = null
        lockFileAccess.close()
        lockedFiles.remove(target)
      })
      stoppables.forEach(Consumer { it: Runnable ->
        try {
          it.run()
        } catch (e: Throwable) {
          throw RuntimeException(e);
        }
      })
    }

    /**
     * This method acquires a lock on the lock file.
     *
     * Lock file is [java.io.RandomAccessFile] that has two regions:
     * - lock state region, locked for the duration of the operation
     * - lock info region, locked just to write the lock info or read info from it
     *
     * Algorithm:
     * 1. We first try to acquire a lock on the state region with retries, see [lockStateRegion].
     * 2. ...
     *    1. If we use exclusive lock, and we succeed in step 1., then we acquire an exclusive lock
     * on the information region and write our details (port and lock id) there, and then we release lock of information region.
     * That way other processes can read our details and ping us. That is important for [FileLockManager.LockMode.OnDemand] mode.
     *    2. If we use shared lock, and we succeed in step 1., then we just hold the lock. We don't write anything to the information region
     * since multiple processes can acquire shared lock (due to that we currently also don't support on demand shared locks).
     *    3. If we fail, we throw a timeout exception.
     *
     * On close, we remove our details from info region and release the exclusive lock on the state region.
     *
     * Note: In the implementation we use [java.nio.channels.FileLock] that is tight to a JVM process, not a thread.
     */
    @Throws(Throwable::class)
    fun lock(lockMode: FileLockManager.LockMode): LockState {
      println(String.format("Waiting to acquire %s lock on %s.", lockMode.toString().lowercase(), displayName))

      // Lock the state region, with the requested mode
      val lockOutcome = lockStateRegion(lockMode)
      if (!lockOutcome.isLockWasAcquired) {
        val lockInfo = readInformationRegion(newExponentialBackoff())
        throw timeoutException(
          displayName,
          operationDisplayName,
          lockFile,
          metaDataProvider.processIdentifier,
          lockOutcome,
          lockInfo
        )
      }

      val stateRegionLock = lockOutcome.fileLock
      try {
        val lockState: LockState
        if (!stateRegionLock.isShared) {
          // We have an exclusive lock (whether we asked for it or not).

          // Update the state region

          lockState = lockFileAccess.ensureLockState()

          // Acquire an exclusive lock on the information region and write our details there
          val informationRegionLockOutcome =
            lockInformationRegion(FileLockManager.LockMode.Exclusive, newExponentialBackoff())
          check(informationRegionLockOutcome.isLockWasAcquired) {
            String.format(
              "Unable to lock the information region for %s",
              displayName
            )
          }
          // check that the length of the reserved region is enough for storing our content
          try {
            lockFileAccess.writeLockInfo(port, lockId, metaDataProvider.processIdentifier, operationDisplayName)
          } finally {
            informationRegionLockOutcome.fileLock.release()
          }
        } else {
          // Just read the state region
          lockState = lockFileAccess.readLockState()
        }
        println(String.format("Lock acquired on %s.", displayName))
        lock = stateRegionLock
        return lockState
      } catch (t: Throwable) {
        stateRegionLock.release()
        throw t
      }
    }

    fun timeoutException(
      lockDisplayName: String,
      thisOperation: String,
      lockFile: File,
      thisProcessPid: String,
      fileLockOutcome: FileLockOutcome,
      lockInfo: LockInfo
    ): LockTimeoutException {
      if (fileLockOutcome === FileLockOutcome.LOCKED_BY_ANOTHER_PROCESS) {
        val message = String.format(
          "Timeout waiting to lock $lockDisplayName. It is currently in use by another Gradle instance.%nOwner PID: %s%nOur PID: %s%nOwner Operation: %s%nOur operation: %s%nLock file: %s",
          lockInfo.pid,
          thisProcessPid,
          lockInfo.operation,
          thisOperation,
          lockFile
        )
        return LockTimeoutException(message, lockFile)
      } else if (fileLockOutcome === FileLockOutcome.LOCKED_BY_THIS_PROCESS) {
        val message = String.format(
          "Timeout waiting to lock $lockDisplayName. It is currently in use by this Gradle process.Owner Operation: %s%nOur operation: %s%nLock file: %s",
          lockInfo.operation,
          thisOperation,
          lockFile
        )
        return LockTimeoutException(message, lockFile)
      } else {
        throw IllegalArgumentException("Unexpected lock outcome: $fileLockOutcome")
      }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun readInformationRegion(backoff: ExponentialBackoff<AwaitableFileLockReleasedSignal>): LockInfo {
      // Can't acquire lock, get details of owner to include in the error message
      var out = LockInfo()
      val lockOutcome = lockInformationRegion(FileLockManager.LockMode.Shared, backoff)
      if (!lockOutcome.isLockWasAcquired) {
        println("Could not lock information region for $displayName. Ignoring.")
      } else {
        try {
          out = lockFileAccess.readLockInfo()
        } finally {
          lockOutcome.fileLock.release()
        }
      }
      return out
    }

    /**
     * Method that tries to acquire a lock on the state region of a lock file.
     * <br></br><br></br>
     *
     * If acquiring the lock for the state region fails, we read information region and get the port (if present) and we send a ping request to the owner
     * (see [FileLockContentionHandler.maybePingOwner] how ping algorithm is done).
     * We then repeat the process with exponential backoff, till we finally acquire the lock or timeout (by default in [DefaultFileLockManager.DEFAULT_LOCK_TIMEOUT]).
     */
    @Throws(IOException::class, InterruptedException::class)
    fun lockStateRegion(lockMode: FileLockManager.LockMode): FileLockOutcome {
      val backoff = newExponentialBackoff(lockTimeout)
      return backoff.retryUntil(object : ExponentialBackoff.Query<FileLockOutcome> {
        private var lastPingTime: Long = 0
        private var lastLockHolderPort = 0

        @Throws(IOException::class, InterruptedException::class)
        override fun run(): ExponentialBackoff.Result<FileLockOutcome> {
          val lockOutcome = lockFileAccess.tryLockState(lockMode == FileLockManager.LockMode.Shared)
          if (lockOutcome.isLockWasAcquired) {
            return successful(lockOutcome)
          }
          if (port != -1) { //we don't like the assumption about the port very much
            val lockInfo = readInformationRegion(backoff)
            if (lockInfo.port != -1) {
              if (lockInfo.port != lastLockHolderPort) {
                backoff.restartTimer()
                lastLockHolderPort = lockInfo.port
                lastPingTime = 0
              }
              if (fileLockContentionHandler.maybePingOwner(
                  lockInfo.port,
                  lockInfo.lockId,
                  displayName,
                  backoff.timer.elapsedMillis - lastPingTime,
                  backoff.signal
                )
              ) {
                lastPingTime = backoff.timer.elapsedMillis
                println("The file lock for $displayName is held by a different Gradle process (pid: ${lockInfo.pid}, lockId: ${lockInfo.lockId}). Pinged owner at port ${lockInfo.port}")
              }
            } else {
              println("The file lock for $displayName is held by a different Gradle process. I was unable to read on which port the owner listens for lock access requests.")
            }
          }
          return notSuccessful(lockOutcome)
        }
      })
    }

    @Throws(IOException::class, InterruptedException::class)
    fun lockInformationRegion(
      lockMode: FileLockManager.LockMode,
      backoff: ExponentialBackoff<AwaitableFileLockReleasedSignal>
    ): FileLockOutcome {
      return backoff.retryUntil {
        val lockOutcome = lockFileAccess.tryLockInfo(lockMode == FileLockManager.LockMode.Shared)
        if (lockOutcome.isLockWasAcquired) {
          return@retryUntil successful<FileLockOutcome>(lockOutcome)
        } else {
          return@retryUntil notSuccessful<FileLockOutcome>(lockOutcome)
        }
      }
    }
  }

  private fun newExponentialBackoff(
    shortTimeout: Duration = this.shortTimeout,
  ): ExponentialBackoff<AwaitableFileLockReleasedSignal> {
    return ExponentialBackoff.of(shortTimeout, AwaitableFileLockReleasedSignal())
  }

  //    @VisibleForTesting
  internal class AwaitableFileLockReleasedSignal : FileLockReleasedSignal, ExponentialBackoff.Signal {
    private val lock: Lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()
    private var waiting = 0

    @Throws(InterruptedException::class)
    override fun await(period: Duration): Boolean {
      lock.lock()
      try {
        waiting++
        return condition.await(period.inWholeMilliseconds, TimeUnit.MILLISECONDS)
      } finally {
        waiting--
        lock.unlock()
      }
    }

    override fun trigger() {
      lock.lock()
      try {
        if (waiting > 0) {
          condition.signalAll()
        }
      } finally {
        lock.unlock()
      }
    }

    //        @VisibleForTesting
    fun isWaiting(): Boolean {
      lock.lock()
      try {
        return waiting > 0
      } finally {
        lock.unlock()
      }
    }
  }

  companion object {
    //    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFileLockManager.class);
    val DEFAULT_LOCK_TIMEOUT: Duration = 60000.milliseconds

    fun determineLockTargetFile(target: File): File {
      return if (target.isDirectory) {
        File(target, target.name + ".lock")
      } else {
        File(target.parentFile, target.name + ".lock")
      }
    }
  }
}
