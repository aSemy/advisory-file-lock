import java.util.function.Function
import java.util.function.Supplier

interface Cache<K, V> {
  /**
   * Locates the given entry, using the supplied factory when the entry is not present or has been discarded, to recreate the entry in the cache.
   *
   *
   * Implementations may prevent more than one thread calculating the same key at the same time or not.
   */
  fun get(key: K, factory: Function<in K, out V>?): V

  fun get(key: K, supplier: Supplier<out V>): V {
    return get(key) { _: K -> supplier.get() }
  }

  /**
   * Locates the given entry, if present. Returns `null` when missing.
   */
  fun getIfPresent(key: K): V?

  /**
   * Adds the given value to the cache, replacing any existing value.
   */
  fun put(key: K, value: V)
}
