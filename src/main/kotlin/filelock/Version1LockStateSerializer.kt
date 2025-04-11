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

/**
 * An older, cross-version state info format.
 */
class Version1LockStateSerializer : LockStateSerializer {
  override val size: Int
    get() = 1

  override val version: Byte
    get() = 1

  override fun createInitialState(): LockState {
    return DirtyFlagLockState(true)
  }

  @Throws(IOException::class)
  override fun write(dataOutput: DataOutput, lockState: LockState) {
    val state = lockState as DirtyFlagLockState
    dataOutput.writeBoolean(!state.isDirty)
  }

  @Throws(IOException::class)
  override fun read(dataInput: DataInput): LockState {
    return DirtyFlagLockState(!dataInput.readBoolean())
  }

  private class DirtyFlagLockState(override val isDirty: Boolean) : LockState {
    override fun canDetectChanges(): Boolean {
      return false
    }

    override val isInInitialState: Boolean
      get() = false

    override fun beforeUpdate(): LockState {
      return DirtyFlagLockState(true)
    }

    override fun completeUpdate(): LockState {
      return DirtyFlagLockState(false)
    }

    override fun hasBeenUpdatedSince(state: FileLock.State): Boolean {
      throw UnsupportedOperationException("This protocol version does not support detecting changes by other processes.")
    }
  }
}
