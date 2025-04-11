interface LockOptions {
  val mode: FileLockManager.LockMode

  val isUseCrossVersionImplementation: Boolean

  /**
   * Creates a copy of this options instance using the given mode.
   *
   * @param mode the mode to overwrite the current mode with
   */
  fun copyWithMode(mode: FileLockManager.LockMode): LockOptions
}
