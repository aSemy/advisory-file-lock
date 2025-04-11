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

import filelock.FileLockOutcome.Companion.acquired
import files.RandomAccessFileInputStream
import files.RandomAccessFileOutputStream
import java.io.*
import java.nio.channels.OverlappingFileLockException
import kotlin.math.min

class LockInfoAccess(private val infoRegionPos: Long) {
  private val lockInfoSerializer = LockInfoSerializer()

  @Throws(IOException::class)
  fun readLockInfo(lockFileAccess: RandomAccessFile): LockInfo {
    if (lockFileAccess.length() <= infoRegionPos) {
      return LockInfo()
    } else {
      lockFileAccess.seek(infoRegionPos)

      val inputStream = DataInputStream(BufferedInputStream(RandomAccessFileInputStream(lockFileAccess)))
      val protocolVersion = inputStream.readByte()
      check(protocolVersion == lockInfoSerializer.version) {
        String.format(
          "Unexpected lock protocol found in lock file. Expected %s, found %s.",
          lockInfoSerializer.version,
          protocolVersion
        )
      }

      return lockInfoSerializer.read(inputStream)
    }
  }

  @Throws(IOException::class)
  fun writeLockInfo(lockFileAccess: RandomAccessFile, lockInfo: LockInfo) {
    lockFileAccess.seek(infoRegionPos)

    val outstr = DataOutputStream(BufferedOutputStream(RandomAccessFileOutputStream(lockFileAccess)))
    outstr.writeByte(lockInfoSerializer.version.toInt())
    lockInfoSerializer.write(outstr, lockInfo)
    outstr.flush()

    lockFileAccess.setLength(lockFileAccess.filePointer)
  }

  @Throws(IOException::class)
  fun clearLockInfo(lockFileAccess: RandomAccessFile) {
    lockFileAccess.setLength(min(lockFileAccess.length().toDouble(), infoRegionPos.toDouble()).toLong())
  }

  @Throws(IOException::class)
  fun tryLock(lockFileAccess: RandomAccessFile, shared: Boolean): FileLockOutcome {
    try {
      val fileLock = lockFileAccess.channel.tryLock(infoRegionPos, INFORMATION_REGION_SIZE - infoRegionPos, shared)
      return if (fileLock == null) {
        FileLockOutcome.LOCKED_BY_ANOTHER_PROCESS
      } else {
        acquired(fileLock)
      }
    } catch (e: OverlappingFileLockException) {
      // Locked by this process, treat as not acquired
      return FileLockOutcome.LOCKED_BY_THIS_PROCESS
    }
  }

  companion object {
    const val INFORMATION_REGION_SIZE: Int = 2052
  }
}
