package dev.adamko.advisoryfilelock

sealed interface LockAccess : AutoCloseable {

  /**
   * Acquires the lock.
   *
   * Does nothing if the lock is already acquired.
   *
   * Blocks the current thread until the lock is acquired,
   * or the thread is interrupted.
   */
  fun lock()

  /**
   * Releases the lock.
   *
   * Might block the current thread until the lock is released,
   * or the thread is interrupted.
   *
   * Does nothing if the lock is already released.
   */
  fun unlock()

  /**
   * Calls [unlock].
   *
   * Can be used as a resource in a `use {}` block.
   */
  override fun close(): Unit =
    unlock()
}
