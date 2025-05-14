package dev.adamko.lokka

sealed interface LockAccess : AutoCloseable {
  fun lock()
  fun unlock()
  override fun close(): Unit = unlock()
}
