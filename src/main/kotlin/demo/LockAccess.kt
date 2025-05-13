package demo

internal sealed interface LockAccess : AutoCloseable {
  fun lock()
  fun unlock()
  override fun close(): Unit = unlock()
}
