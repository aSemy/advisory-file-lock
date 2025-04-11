/**
 * Indicates that the integrity of a file has been violated or cannot be guaranteed.
 */
class FileIntegrityViolationException(message: String) : RuntimeException(message)
