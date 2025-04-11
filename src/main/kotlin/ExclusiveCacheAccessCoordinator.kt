import java.util.function.Supplier

/**
 * Provides synchronised access to a cache.
 */
interface ExclusiveCacheAccessCoordinator {
  /**
   * Performs some work against the cache. Acquires exclusive locks on the appropriate resources, so that the given action is the only action to execute across all processes (including this one). Releases the locks and all resources at the end of the action.
   *
   *
   * This method is re-entrant, so that an action can call back into this method.
   *
   *
   * Note: this method differs from [withFileLock] in that this method also blocks other threads from this process and all threads from other processes from accessing the cache.
   */
  fun <T> useCache(action: Supplier<out T>): T

  /**
   * Performs some work against the cache. Acquires exclusive locks on the appropriate resources, so that the given action is the only action to execute across all processes (including this one). Releases the locks and all resources at the end of the action.
   *
   *
   * This method is re-entrant, so that an action can call back into this method.
   *
   *
   * Note: this method differs from [withFileLock] in that this method also blocks other threads from this process and all threads from other processes from accessing the cache.
   */
  fun useCache(action: Runnable)

  /**
   * Performs some work against the cache. Acquires exclusive locks on the appropriate file resources, so that only the actions from this process may run. Releases the locks and all resources at the end of the action. Allows other threads from this process to execute, but does not allow any threads from any other process to run.
   *
   *
   * This method is re-entrant, so that an action can call back into this method.
   */
  fun <T> withFileLock(action: Supplier<out T>): T

  /**
   * Performs some work against the cache. Acquires exclusive locks on the appropriate file resources, so that only the actions from this process may run. Releases the locks and all resources at the end of the action. Allows other threads from this process to execute, but does not allow any threads from any other process to run.
   *
   *
   * This method is re-entrant, so that an action can call back into this method.
   */
  fun withFileLock(action: Runnable)
}
