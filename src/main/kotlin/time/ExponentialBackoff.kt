package time

import java.io.IOException
import java.util.*
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ExponentialBackoff<S : ExponentialBackoff.Signal> private constructor(
  private val timeout: Duration,
  val signal: S,
  private val slotTime: Duration = SLOT_TIME,
) {
  private val random = Random()

  lateinit var timer: CountdownTimer
    private set

  init {
    restartTimer()
  }

  fun restartTimer() {
    timer = Time.startCountdownTimer(timeout.inWholeMilliseconds)
  }

  /**
   * Retries the given query until it returns a 'successful' result.
   *
   * @param query which returns non-null value when successful.
   * @param <T> the result type.
   * @return the last value returned by the query.
   * @throws IOException thrown by the query.
   * @throws InterruptedException if interrupted while waiting.
   */
  @Throws(IOException::class, InterruptedException::class)
  fun <T : Any> retryUntil(query: Query<T>): T {
    var iteration = 0
    var result: Result<T>
    while (!(query.run().also { result = it }).isSuccessful) {
      if (timer.hasExpired()) {
        break
      }
      val signaled = signal.await(backoffPeriodFor(++iteration))
      if (signaled) {
        iteration = 0
      }
    }
    return result.value
  }

  private fun backoffPeriodFor(iteration: Int): Duration {
    val millis = random.nextInt(min(iteration, CAP_FACTOR)) * slotTime.inWholeMilliseconds
    return millis.milliseconds
  }

  fun interface Signal {
    @Throws(InterruptedException::class)
    fun await(period: Duration): Boolean

    object SLEEP : Signal {
      @Throws(InterruptedException::class)
      override fun await(period: Duration): Boolean {
        Thread.sleep(period.inWholeMilliseconds)
        return false
      }
    }

    companion object
  }

  fun interface Query<T> {
    @Throws(IOException::class, InterruptedException::class)
    fun run(): Result<T>
  }

  abstract class Result<T> {
    abstract val isSuccessful: Boolean

    abstract val value: T

    companion object {
      /**
       * Creates a result that indicates that the operation was successful and should not be repeated.
       */
      fun <T : Any> successful(value: T): Result<T> {
        return object : Result<T>() {
          override val isSuccessful: Boolean
            get() = true

          override val value: T
            get() = value
        }
      }

      /**
       * Creates a result that indicates that the operation was not successful and should be repeated.
       */
      fun <T : Any> notSuccessful(value: T): Result<T> {
        return object : Result<T>() {
          override val isSuccessful: Boolean
            get() = false

          override val value: T
            get() = value
        }
      }
    }
  }

  companion object {
    private const val CAP_FACTOR = 100
    private val SLOT_TIME: Duration = 25.milliseconds

    fun of(timeout: Duration): ExponentialBackoff<Signal> {
      return ExponentialBackoff(timeout, Signal.SLEEP)
    }

    fun <T : Signal> of(timeout: Duration, signal: T): ExponentialBackoff<T> {
      return ExponentialBackoff(
        timeout,
        signal,
//        SLOT_TIME
      )
    }

    fun of(timeout: Duration, slotTime: Duration): ExponentialBackoff<Signal> {
      return ExponentialBackoff(
        timeout = timeout,
        signal = Signal.SLEEP,
        slotTime = slotTime,
      )
    }
  }
}
