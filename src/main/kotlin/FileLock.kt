import java.io.Closeable
import java.nio.file.Path

interface FileLock : Closeable, FileAccess {
  /**
   * Returns true if the most recent mutation method ([.updateFile] or [.writeFile] attempted by any process succeeded
   * (ie a process did not crash while updating the target file).
   *
   * Returns false if no mutation method has ever been called for the target file.
   */
  val unlockedCleanly: Boolean

  /**
   * Returns true if the given file is used by this lock.
   */
  fun isLockFile(file: Path): Boolean

  /**
   * Closes this lock, releasing the lock and any resources associated with it.
   */
  override fun close()

  /**
   * Returns some memento of the current state of this target file.
   */
  val state: State?

  /**
   * The actual mode of the lock. May be different to what was requested.
   */
  val mode: FileLockManager.LockMode

  /**
   * An immutable snapshot of the state of a lock.
   */
  interface State {
    fun canDetectChanges(): Boolean
    val isInInitialState: Boolean
    fun hasBeenUpdatedSince(state: State): Boolean
  }
}
