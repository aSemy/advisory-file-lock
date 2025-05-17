package dev.adamko.advisoryfilelock

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
