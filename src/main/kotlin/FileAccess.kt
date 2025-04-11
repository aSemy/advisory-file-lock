import java.util.concurrent.Callable
import java.util.function.Supplier

/**
 * Provides synchronization with other processes for a particular file.
 */
interface FileAccess {
  /**
   * Runs the given action under a shared or exclusive lock on the target file.
   *
   * @throws LockTimeoutException On timeout acquiring lock, if required.
   * @throws IllegalStateException When this lock has been closed.
   * @throws FileIntegrityViolationException If the integrity of the file cannot be guaranteed (i.e. [.writeFile] has never been called)
   * @throws InsufficientLockModeException If the held lock is not at least a shared lock (e.g. LockMode.NONE)
   */
  @Throws(LockTimeoutException::class, FileIntegrityViolationException::class, InsufficientLockModeException::class)
  fun <T> readFile(action: Callable<out T>): T

  /**
   * Runs the given action under a shared or exclusive lock on the target file.
   *
   * @throws LockTimeoutException On timeout acquiring lock, if required.
   * @throws IllegalStateException When this lock has been closed.
   * @throws FileIntegrityViolationException If the integrity of the file cannot be guaranteed (i.e. [.writeFile] has never been called)
   * @throws InsufficientLockModeException If the held lock is not at least a shared lock (e.g. LockMode.NONE)
   */
  @Throws(LockTimeoutException::class, FileIntegrityViolationException::class, InsufficientLockModeException::class)
  fun <T> readFile(action: Supplier<out T>): T

  /**
   * Runs the given action under an exclusive lock on the target file. If the given action fails, the lock is marked as uncleanly unlocked.
   *
   * @throws LockTimeoutException On timeout acquiring lock, if required.
   * @throws IllegalStateException When this lock has been closed.
   * @throws FileIntegrityViolationException If the integrity of the file cannot be guaranteed (i.e. [.writeFile] has never been called)
   * @throws InsufficientLockModeException If the held lock is not an exclusive lock.
   */
  @Throws(LockTimeoutException::class, FileIntegrityViolationException::class, InsufficientLockModeException::class)
  fun updateFile(action: Runnable)

  /**
   * Runs the given action under an exclusive lock on the target file, without checking its integrity. If the given action fails, the lock is marked as uncleanly unlocked.
   *
   *
   * This method should be used when it is of no consequence if the target was not previously unlocked, e.g. the content is being replaced.
   *
   *
   * Besides not performing integrity checking, this method shares the locking semantics of [.updateFile]
   *
   * @throws LockTimeoutException On timeout acquiring lock, if required.
   * @throws IllegalStateException When this lock has been closed.
   * @throws InsufficientLockModeException If the held lock is not an exclusive lock.
   */
  @Throws(LockTimeoutException::class, InsufficientLockModeException::class)
  fun writeFile(action: Runnable)
}
