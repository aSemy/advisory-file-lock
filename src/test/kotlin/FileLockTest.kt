package dev.adamko.advisoryfilelock

import dev.adamko.advisoryfilelock.internal.threadSleep
import io.kotest.assertions.asClue
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir

class FileLockTest {

  @Test
  fun `verify write lock prevents concurrent modification`(
    @TempDir
    workingDir: Path,
  ): Unit = runBlocking(Dispatchers.IO) {
    val lockFile = LockFile(workingDir.resolve("a.lock"))

    var counter = 0

    val n = 1000  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine

    lockFile.use { locker ->
      repeat(n) {
        coroutineScope {
          launch {
            repeat(k) {
              locker.withWriteLock {
                counter++
              }
            }
          }
        }
      }
    }

    assertEquals(n * k, counter)
  }

  @Test
  fun `when read lock thread is interrupted - expect obtaining read lock throws cancellation exception`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFile = LockFile(workingDir.resolve("a.lock"))

    lockFile.writeLock().lock()

    var thrown: Throwable? = null

    val t = thread {
      try {
        println("acquiring read lock...")
        lockFile.withReadLock {
          println("acquired read lock!")
          error("read lock acquisition should be interrupted")
        }
      } catch (e: Throwable) {
        thrown = e
      }
    }

    threadSleep(100.milliseconds)
    t.interrupt()
    t.join(100)

    if (t.isAlive) {
      fail("Function did not respond to interrupt, thread is still running")
    }

    thrown.asClue {
      it.shouldBeInstanceOf<InterruptedException>()
      it.message shouldStartWith "Interrupted while waiting for lock on FileChannel@"
    }
  }

  @Test
  fun `expect two read locks can be obtained`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFile = LockFile(workingDir.resolve("a.lock"))

    var twoReadLockObtained = false

    val t = thread {
      lockFile.withReadLock {
        lockFile.withReadLock {
          twoReadLockObtained = true
        }
      }
    }

    t.join(100)

    assertTrue(twoReadLockObtained, "expect two read locks are obtained")
  }

  @Test
  fun `when read lock is active - expect write lock cannot be obtained`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFile = LockFile(workingDir.resolve("a.lock"))

    var writeLockObtained = false

    val t = thread {
      try {
        lockFile.withReadLock {
          lockFile.withWriteLock {
            writeLockObtained = true
          }
        }
      } catch (_: InterruptedException) {
        // ignore - an interrupt is expected
      }
    }

    t.join(100)
    t.interrupt()

    assertFalse(writeLockObtained, "expect write lock was not obtained")
  }

  @Test
  fun `when write lock is active - expect read lock cannot be obtained`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFile = LockFile(workingDir.resolve("a.lock"))

    var readLockObtained = false

    val t = thread {
      try {
        lockFile.withWriteLock {
          lockFile.withReadLock {
            readLockObtained = true
          }
        }
      } catch (_: InterruptedException) {
        // ignore - an interrupt is expected
      }
    }

    t.join(100)
    t.interrupt()

    assertFalse(readLockObtained, "expect read lock was not obtained")
  }

  @Test
  fun `when FileReadWriteLock is closed - expect access file is closed`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFilePath = workingDir.resolve("a.lock")
    val lockFile = LockFile(workingDir.resolve("a.lock"))

    // TODO
  }

  @Test
  fun `when exception is thrown from within read lock use block - expect read lock is released`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFilePath = workingDir.resolve("a.lock")
    val lockFile = LockFile(lockFilePath)

    // TODO
  }

  @Test
  fun `when exception is thrown from within read lock use block - expect write lock is released`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFile = LockFile(workingDir.resolve("a.lock"))
    // TODO
  }

  @Test
  fun `verify temp dir can be modified using system property`(
    @TempDir
    workingDir: Path,
  ) {
    val lockFile = LockFile(workingDir.resolve("a.lock"))
    // TODO test dev.adamko.advisoryfilelock.socketDir
  }
}
