/**
 * Signal that a file lock has been released.
 *
 * @see locklistener.FileLockContentionHandler
 */
interface FileLockReleasedSignal {
  /**
   * Triggers this signal to notify the lock requesters that the file
   * lock has been released.
   *
   *
   * Returns once the signal has been emitted but not necessarily
   * received by the lock requesters.
   */
  fun trigger()
}
