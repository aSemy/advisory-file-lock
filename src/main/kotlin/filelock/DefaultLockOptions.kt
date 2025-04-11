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

import FileLockManager
import LockOptions

internal class DefaultLockOptions private constructor(
  override val mode: FileLockManager.LockMode,
  override var isUseCrossVersionImplementation: Boolean
) :
  LockOptions {
  fun useCrossVersionImplementation(): DefaultLockOptions {
    isUseCrossVersionImplementation = true
    return this
  }

  override fun copyWithMode(mode: FileLockManager.LockMode): LockOptions {
    return DefaultLockOptions(mode, isUseCrossVersionImplementation)
  }

  override fun toString(): String {
    return "DefaultLockOptions{" +
        "mode=" + mode +
        ", crossVersion=" + isUseCrossVersionImplementation +
        '}'
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is DefaultLockOptions) {
      return false
    }

    if (isUseCrossVersionImplementation != other.isUseCrossVersionImplementation) {
      return false
    }
    if (mode != other.mode) {
      return false
    }

    return true
  }

  override fun hashCode(): Int {
    var result = mode.hashCode()
    result = 31 * result + (if (isUseCrossVersionImplementation) 1 else 0)
    return result
  }

  companion object {
    fun mode(lockMode: FileLockManager.LockMode): DefaultLockOptions {
      return DefaultLockOptions(lockMode, false)
    }
  }
}
