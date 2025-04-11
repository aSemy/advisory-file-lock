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
import java.io.*
import java.nio.channels.OverlappingFileLockException


class LockStateAccess(private val protocol: LockStateSerializer) {
  val regionEnd: Int

  init {
    regionEnd = STATE_CONTENT_START + protocol.size
  }

  @Throws(IOException::class)
  fun ensureLockState(lockFileAccess: RandomAccessFile): LockState {
    if (lockFileAccess.length() == 0L) {
      // File did not exist before locking, use some initial state
      val state = protocol.createInitialState()
      writeState(lockFileAccess, state)
      return state
    } else {
      return readState(lockFileAccess)
    }
  }

  @Throws(IOException::class)
  fun writeState(lockFileAccess: RandomAccessFile, lockState: LockState) {
    val outstr = ByteArrayOutputStream()
    val dataOutput = DataOutputStream(outstr)
    dataOutput.writeByte(protocol.version.toInt())
    protocol.write(dataOutput, lockState)
    dataOutput.flush()

    lockFileAccess.seek(REGION_START.toLong())
    lockFileAccess.write(outstr.toByteArray())
    assert(lockFileAccess.filePointer == regionEnd.toLong())
  }

  @Throws(IOException::class)
  fun readState(lockFileAccess: RandomAccessFile): LockState {
    try {
      val buffer = ByteArray(regionEnd)
      lockFileAccess.seek(REGION_START.toLong())

      var readPos = 0
      while (readPos < buffer.size) {
        val nread = lockFileAccess.read(buffer, readPos, buffer.size - readPos)
        if (nread < 0) {
          break
        }
        readPos += nread
      }

      val inputStream = ByteArrayInputStream(buffer, 0, readPos)
      val dataInput = DataInputStream(inputStream)

      val protocolVersion = dataInput.readByte()
      check(protocolVersion == protocol.version) {
        String.format(
          "Unexpected lock protocol found in lock file. Expected %s, found %s.",
          protocol.version,
          protocolVersion
        )
      }
      return protocol.read(dataInput)
    } catch (e: EOFException) {
      return protocol.createInitialState()
    }
  }

  @Throws(IOException::class)
  fun tryLock(lockFileAccess: RandomAccessFile, shared: Boolean): FileLockOutcome {
    try {
      val fileLock = lockFileAccess.channel.tryLock(REGION_START.toLong(), regionEnd.toLong(), shared)
      return if (fileLock == null) {
        FileLockOutcome.LOCKED_BY_ANOTHER_PROCESS
      } else {
        acquired(fileLock)
      }
    } catch (e: OverlappingFileLockException) {
      return FileLockOutcome.LOCKED_BY_THIS_PROCESS
    }
  }

  companion object {
    private const val REGION_START = 0
    private const val STATE_CONTENT_START = 1
  }
}
