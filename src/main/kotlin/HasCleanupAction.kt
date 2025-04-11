/**
 * A cache entity that can be cleaned on demand.
 */
interface HasCleanupAction {
  /**
   * Cleans up the cache, if any cleanup action has been provided.
   */
  fun cleanup()
}
