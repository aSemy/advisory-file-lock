import java.util.function.Function

/**
 * A persistent store of objects of type V indexed by a key of type K.
 */
interface IndexedCache<K, V> : Cache<K, V> {
  /**
   * Fetches the value mapped to the given key from this cache, blocking until it is available.
   *
   * A shared or exclusive file lock is held while fetching the value, depending on implementation.
   *
   * @return The value, or null if no value associated with the key.
   */
  override fun getIfPresent(key: K): V?

  /**
   * Returns the value mapped to the given key, producing the value if not present.
   *
   * The implementation blocks when multiple threads producing the same value concurrently, so that only a single thread produces the value and the other threads reuse the result.
   *
   * Production of the value always happens synchronously by the calling thread. However, the implementation may update the backing store with new value synchronously or asynchronously.
   *
   * A file lock is held until the value has been produced and written to the persistent store, and other processes will be blocked from producing the same value until this process has completed doing so.
   *
   * @return The value.
   */
  override fun get(key: K, factory: Function<in K, out V>?): V

  /**
   * Maps the given value to the given key, replacing any existing value.
   *
   * The implementation may do this synchronously or asynchronously. A file lock is held until the value has been written to the persistent store.
   */
  override fun put(key: K, value: V)

  /**
   * Removes a key-value mapping from this cache. A shared lock is held while updating the value.
   *
   * The implementation may do this synchronously or asynchronously. A file lock is held until the value has been removed from the persistent store.
   */
  fun remove(key: K)
}
