/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package filelock

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class LockFileAccess(lockFile: File, private val lockStateAccess: LockStateAccess) {
  private val lockFileAccess = RandomAccessFile(lockFile, "rw")

  private val lockInfoAccess = LockInfoAccess(lockStateAccess.regionEnd.toLong())

  @Throws(IOException::class)
  fun close() {
    lockFileAccess.close()
  }

  @Throws(IOException::class)
  fun writeLockInfo(port: Int, lockId: Long, pid: String, operation: String) {
    val lockInfo = LockInfo(
      port,
      lockId,
      pid,
      operation
    )
    lockInfoAccess.writeLockInfo(lockFileAccess, lockInfo)
  }

  @Throws(IOException::class)
  fun readLockInfo(): LockInfo {
    return lockInfoAccess.readLockInfo(lockFileAccess)
  }

  /**
   * Reads the lock state from the lock file, possibly writing out a new lock file if not present or empty.
   */
  @Throws(IOException::class)
  fun ensureLockState(): LockState {
    return lockStateAccess.ensureLockState(lockFileAccess)
  }

  @Throws(IOException::class)
  fun markClean(lockState: LockState): LockState {
    val newState = lockState.completeUpdate()
    lockStateAccess.writeState(lockFileAccess, newState)
    return newState
  }

  @Throws(IOException::class)
  fun markDirty(lockState: LockState): LockState {
    val newState = lockState.beforeUpdate()
    lockStateAccess.writeState(lockFileAccess, newState)
    return newState
  }

  @Throws(IOException::class)
  fun clearLockInfo() {
    lockInfoAccess.clearLockInfo(lockFileAccess)
  }

  @Throws(IOException::class)
  fun tryLockInfo(shared: Boolean): FileLockOutcome {
    return lockInfoAccess.tryLock(lockFileAccess, shared)
  }

  @Throws(IOException::class)
  fun tryLockState(shared: Boolean): FileLockOutcome {
    return lockStateAccess.tryLock(lockFileAccess, shared)
  }

  /**
   * Reads the lock state from the lock file.
   */
  @Throws(IOException::class)
  fun readLockState(): LockState {
    return lockStateAccess.readState(lockFileAccess)
  }

  fun isClosed(): Boolean =
    !lockFileAccess.channel.isOpen

}
