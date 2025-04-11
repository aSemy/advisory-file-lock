import java.util.function.Consumer

interface FileLockContentionHandler {
  fun start(lockId: Long, whenContended: Consumer<FileLockReleasedSignal>)

  fun stop(lockId: Long)

  fun reservePort(): Int

  /**
   * Pings the lock owner with the give port to start the lock releasing
   * process in the owner. May not ping the owner if:
   * - The owner was already pinged about the given lock before and the lock release is in progress
   * - The ping through the underlying socket failed
   *
   * @return `true` if the owner was pinged in this call
   */
  fun maybePingOwner(
    port: Int,
    lockId: Long,
    displayName: String,
    timeElapsed: Long,
     signal: FileLockReleasedSignal?
  ): Boolean

  /**
   * Returns true if the handler is running and communication with other processes is still possible.
   */
  val isRunning: Boolean
}
