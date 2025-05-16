package dev.adamko.advisoryfilelock

import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.createFile
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileLockTest {

  @Test
  fun `verify write lock prevents concurrent modification`(
    @TempDir
    workingDir: Path,
  ): Unit = runTest {
    val lockFile = workingDir.resolve("a.lock")
      .createFile()

    var counter = 0

    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine

    FileReadWriteLock(lockFile).use { locker ->
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
  fun `when read lock thread is cancelled - expect obtaining read lock throws cancellation exception`(
    @TempDir
    workingDir: Path,
  ) {
    // TODO
  }


  @Test
  fun `expect two read locks can be obtained`(
    @TempDir
    workingDir: Path,
  ): Unit = runTest {
    val lockFile = workingDir.resolve("a.lock")
      .createFile()

    val rwl = FileReadWriteLock(lockFile)

    var twoReadLockObtained = false

    val t = thread {
      rwl.withReadLock {
        rwl.withReadLock {
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
  ): Unit = runTest {
    val lockFile = workingDir.resolve("a.lock")
      .createFile()

    val rwl = FileReadWriteLock(lockFile)

    var writeLockObtained = false

    val t = thread {
      rwl.withReadLock {
        rwl.withWriteLock {
          writeLockObtained = true
        }
      }
    }

    t.join(100)
    t.interrupt()

//    threadSleep(1.seconds)

    assertFalse(writeLockObtained, "expect write lock was not obtained")
  }

  @Test
  fun `when write lock is active - expect read lock cannot be obtained`(
    @TempDir
    workingDir: Path,
  ): Unit = runTest {


    val lockFile = workingDir.resolve("a.lock")
      .createFile()

    val rwl = FileReadWriteLock(lockFile)

    var readLockObtained = false

    val t = thread {
      rwl.withWriteLock {
        rwl.withReadLock {
          readLockObtained = true
        }
      }
    }

    t.join(1.seconds.inWholeMilliseconds)
    t.interrupt()

//    threadSleep(1.seconds)

    assertFalse(readLockObtained, "expect read lock was not obtained")

//    val lockFile = workingDir.resolve("a.lock")
//      .createFile()
//
//    val rwl = FileReadWriteLock(lockFile)
//
//    rwl.withWriteLock {
//      val readLockThread = thread(isDaemon = true) {
//        rwl.readLock().lock()
//        fail("read lock should not have been acquired")
//      }
//    }
//    val writeLock = rwl.writeLock()
//    writeLock.lock()
//
//    val readLockThread = thread(isDaemon = true) {
//      rwl.readLock()
//      fail("read lock should not have been acquired")
//    }
//
//    threadSleep(1.seconds)
//
//    readLockThread.interrupt()

    // TODO
  }

  @Test
  fun `when FileReadWriteLock is released - expect access file is closed`() {

    // TODO
  }
}
