package dev.adamko.advisoryfilelock

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Executes the given [action] under the read lock of this lock.
 * @return the return value of [action].
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> LockFile.withReadLock(action: () -> T): T {
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
inline fun <T> LockFile.withWriteLock(action: () -> T): T {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  writeLock().withLock {
    return action()
  }
}
