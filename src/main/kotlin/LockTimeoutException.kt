import java.io.File

/**
 * Thrown on timeout acquiring a lock on a file.
 */
class LockTimeoutException(message: String, val lockFile: File) : RuntimeException(message)
