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

import FileLock
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.*


class DefaultLockStateSerializer : LockStateSerializer {
  override val size: Int
    get() = 16

  override val version: Byte
    get() = 3

  override fun createInitialState(): LockState {
    val creationNumber = Random().nextLong()
    return SequenceNumberLockState(creationNumber, -1, 0)
  }

  @Throws(IOException::class)
  override fun write(lockFileAccess: DataOutput, lockState: LockState) {
    val state = lockState as SequenceNumberLockState
    lockFileAccess.writeLong(state.creationNumber)
    lockFileAccess.writeLong(state.sequenceNumber)
  }

  @Throws(IOException::class)
  override fun read(lockFileAccess: DataInput): LockState {
    val creationNumber = lockFileAccess.readLong()
    val sequenceNumber = lockFileAccess.readLong()
    return SequenceNumberLockState(creationNumber, sequenceNumber, sequenceNumber)
  }

  private class SequenceNumberLockState(
    val creationNumber: Long,
    private val originalSequenceNumber: Long,
    val sequenceNumber: Long
  ) : LockState {

    override fun toString(): String {
      return "[$creationNumber,$sequenceNumber,$isDirty]"
    }

    override fun beforeUpdate(): LockState {
      return SequenceNumberLockState(creationNumber, originalSequenceNumber, 0)
    }

    override fun completeUpdate(): LockState {
      val newSequenceNumber = if (isInInitialState) {
        1
      } else {
        originalSequenceNumber + 1
      }
      return SequenceNumberLockState(creationNumber, newSequenceNumber, newSequenceNumber)
    }

    override val isDirty: Boolean
      get() = sequenceNumber == 0L || sequenceNumber != originalSequenceNumber

    override fun canDetectChanges(): Boolean {
      return true
    }

    override val isInInitialState: Boolean
      get() = originalSequenceNumber <= 0

    override fun hasBeenUpdatedSince(state: FileLock.State): Boolean {
      val other = state as SequenceNumberLockState
      return sequenceNumber != other.sequenceNumber || creationNumber != other.creationNumber
    }
  }
}
