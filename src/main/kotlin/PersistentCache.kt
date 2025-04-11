import java.io.Closeable
import java.nio.file.Path
//import org.gradle.internal.serialize.Serializer

/**
 * Represents a directory that can be used for caching.
 *
 *
 * By default, a shared lock is held on this cache by this process, to prevent it being removed or rebuilt by another process
 * while it is in use. You can change this use [CacheBuilder.withInitialLockMode].
 *
 *
 * You can use [CacheBuilder.withInitializer] to provide an action to initialize the contents
 * of the cache, for building a read-only cache. An exclusive lock is held by this process while the initializer is running.
 *
 *
 * You can also use [.useCache] to perform some action on the cache while holding an exclusive
 * lock on the cache.
 *
 */
interface PersistentCache : ExclusiveCacheAccessCoordinator, Closeable, CleanableStore, HasCleanupAction {
  /**
   * Returns the base directory for this cache.
   */
  override val baseDir: Path

  /**
   * Creates an indexed cache implementation that is contained within this cache. This method may be used at any time.
   *
   *
   * The returned cache may only be used by an action being run from [.useCache].
   * In this instance, an exclusive lock will be held on the cache.
   *
   */
  fun <K, V> createIndexedCache(parameters: IndexedCacheParameters<K, V>?): IndexedCache<K, V>?

//  /**
//   * Creates an indexed cache implementation that is contained within this store. This method may be used at any time.
//   *
//   *
//   * The returned cache may only be used by an action being run from [.useCache].
//   * In this instance, an exclusive lock will be held on the cache.
//   *
//   */
//  fun <K, V> createIndexedCache(name: String?, keyType: Class<K>?, valueSerializer: Serializer<V>): IndexedCache<K, V>?

  fun <K, V> indexedCacheExists(parameters: IndexedCacheParameters<K, V>?): Boolean

  /**
   * Closes this cache, blocking until all operations are complete.
   */
  override fun close()
}
