package dev.adamko.lokka

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import dev.adamko.lokka.internal.*

/**
 * Executes the given [action] under the read lock of this lock.
 * @return the return value of [action].
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> FileReadWriteLock.withReadLock(action: () -> T): T {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  readLock().withLock {
    return action()
  }
}

/**
 * Executes the given [action] under the write lock of this lock.
 * @return the return value of [action].
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> FileReadWriteLock.withWriteLock(action: () -> T): T {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  writeLock().withLock {
    return action()
  }
}

/**
 * Executes the given [action] under this lock.
 * @return the return value of [action].
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> LockAccess.withLock(action: () -> T): T {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  try {
    lock()
    return action()
  } finally {
    unlock()
  }
}
