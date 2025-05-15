package dev.adamko.advisoryfilelock

sealed interface LockAccess : AutoCloseable {
  fun lock()
  fun unlock()
  override fun close(): Unit = unlock()
}
