package dev.adamko.advisoryfilelock

sealed interface LockAccess : AutoCloseable {

  abstract class ReadLock internal constructor() : LockAccess

  abstract class WriteLock internal constructor() : LockAccess

  /**
   * Acquires the lock.
   *
   * Does nothing if the lock is already acquired.
   *
   * Blocks the current thread until the lock is acquired.
   *
   * Throws [InterruptedException] if the current thread is interrupted
   * before the lock is acquired.
   */
  @Throws(InterruptedException::class)
  fun lock()

  /**
   * Releases the lock.
   *
   * Might block the current thread until the lock is released.
   *
   * Throws [InterruptedException] if the current thread is interrupted
   * before the lock is released.
   *
   * Does nothing if the lock is already released.
   */
  @Throws(InterruptedException::class)
  fun unlock()

  /**
   * Calls [unlock].
   *
   * Can be used as a resource in a `use {}` block.
   */
  @Throws(InterruptedException::class)
  override fun close(): Unit =
    unlock()


}
