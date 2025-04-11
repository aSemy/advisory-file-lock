import java.io.File
import java.nio.file.Path

/**
 * Represents file-based store that can be cleaned by a [CleanupAction].
 */
interface CleanableStore {
  /**
   * Returns the base directory that should be cleaned for this store.
   */
  val baseDir: Path

  /**
   * Returns the files used by this store for internal tracking
   * which should be exempt from the cleanup.
   */
  val reservedCacheFiles: Collection<Path>

  val displayName: String
}
