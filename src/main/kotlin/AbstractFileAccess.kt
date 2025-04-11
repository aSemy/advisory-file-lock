import java.util.concurrent.Callable
import java.util.function.Supplier

abstract class AbstractFileAccess : FileAccess {
  @Throws(LockTimeoutException::class, FileIntegrityViolationException::class)
  override fun <T> readFile(action: Callable<out T>): T {
    return readFile(Supplier {
      try {
        return@Supplier action.call()
      } catch (e: Exception) {
        throw RuntimeException(e)
      }
    }  )
  }
}
