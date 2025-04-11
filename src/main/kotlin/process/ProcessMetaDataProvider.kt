package process

/**
 * Provides meta-data about the current process. Generally used for logging and error messages.
 */
interface ProcessMetaDataProvider {
  /**
   * Returns a unique identifier for this process. Should be unique across all processes on the local machine.
   */
//  @JvmField
  val processIdentifier: String

  /**
   * Returns a display name for this process. Should allow a human to figure out which process the display name refers to.
   */
  val processDisplayName: String?
}
