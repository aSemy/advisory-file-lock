/*
 * Copyright 2021 the original author or authors.
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

import java.nio.channels.FileLock

sealed class FileLockOutcome {
  open val isLockWasAcquired: Boolean
    get() = false

  open val fileLock: FileLock
    get() {
      throw IllegalStateException("Lock was not acquired")
    }

  data object LockedByAnotherProcess : FileLockOutcome()
  data object LockedByThisProcess : FileLockOutcome()
  data class Acquired(override val fileLock: FileLock) : FileLockOutcome() {
    override val isLockWasAcquired: Boolean = true
  }

  companion object {
    @JvmField
    val LOCKED_BY_ANOTHER_PROCESS: FileLockOutcome = LockedByAnotherProcess
    @JvmField
    val LOCKED_BY_THIS_PROCESS: FileLockOutcome = LockedByThisProcess

    @JvmStatic
    fun acquired(fileLock: FileLock): FileLockOutcome {
      return Acquired(fileLock)
//      return object : FileLockOutcome() {
//        override val isLockWasAcquired: Boolean
//          get() = true
//
//        override val fileLock: FileLock
//          get() = fileLock
//      }
    }
  }
}
