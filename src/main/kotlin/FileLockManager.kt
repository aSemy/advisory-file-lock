import java.io.File
import java.util.function.Consumer

interface FileLockManager {
//  /**
//   * Creates a lock for the given file with the given mode. Acquires a lock with the given mode, which is held until the lock is
//   * released by calling [FileLock.close]. This method blocks until the lock can be acquired.
//   *
//   * @param target The file to be locked.
//   * @param options The lock options.
//   * @param targetDisplayName A display name for the target file. This is used in log and error messages.
//   */
//  @Throws(LockTimeoutException::class)
//  fun lock(target: File?, options: LockOptions?, targetDisplayName: String?): FileLock?


  /**
   * Creates a lock for the given file with the given mode. Acquires a lock with the given mode, which is held until the lock is
   * released by calling [FileLock.close]. This method blocks until the lock can be acquired.
   *
   *
   * Enable other processes to request access to the provided lock. Provided action runs when the lock access request is received
   * (it means that the lock is contended).
   *
   * @param target The file to be locked.
   * @param options The lock options.
   * @param targetDisplayName A display name for the target file. This is used in log and error messages.
   * @param operationDisplayName A display name for the operation being performed on the target file. This is used in log and error messages.
   * @param whenContended will be called asynchronously by the thread that listens for cache access requests, when such request is received.
   * Note: currently, implementations are permitted to invoke the action *after* the lock as been closed.
   */
  @Throws(LockTimeoutException::class)
  fun lock(
    target: File,
    options: LockOptions,
    targetDisplayName: String,
    operationDisplayName: String = "",
    whenContended: Consumer<FileLockReleasedSignal>? = null,
  ): FileLock

  /**
   * These modes can be used either with [FileLockManager] or when creating [PersistentCache] via [CacheBuilder.withInitialLockMode]
   */
  enum class LockMode {
    /**
     * On demand, single writer, no readers (on demand exclusive mode).
     * <br></br><br></br>
     *
     * Supports processes asking for access. Only one process can access the cache at a time.
     * <br></br><br></br>
     *
     * For [PersistentCache] the file lock is created with [PersistentCache.useCache] or [PersistentCache.withFileLock] method.
     * Lock is released when another process requests it via ping requests, see [DefaultFileLockManager.DefaultFileLock.lock].
     * <br></br><br></br>
     *
     * Not supported by [FileLockManager].
     */
    OnDemand,

    /**
     * Multiple readers, no writers.
     *
     * Used for read-only file locks/caches, many processes can access the lock target/cache concurrently.
     *
     * For [PersistentCache] this is the default behaviour and the cache lock is created when cache is opened.
     * To release a lock a cache has to be closed.
     */
    Shared,

    /**
     * Single writer, no readers.
     *
     * Used for lock targets/caches that are written to. Only one process can access the lock target/cache at a time.
     *
     * For [PersistentCache] the cache lock is created when cache is opened. To release a lock a cache has to be closed.
     */
    Exclusive,

    /**
     * No locking whatsoever.
     */
    None
  }
}
