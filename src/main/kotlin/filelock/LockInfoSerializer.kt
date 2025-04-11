package filelock

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

class LockInfoSerializer {
  val version: Byte
    get() = 3

  @Throws(IOException::class)
  fun write(dataOutput: DataOutput, lockInfo: LockInfo) {
    dataOutput.apply {
      writeInt(lockInfo.port)
      writeLong(lockInfo.lockId)
      writeUTF(trimIfNecessary(lockInfo.pid))
      writeUTF(trimIfNecessary(lockInfo.operation))
    }
  }

  @Throws(IOException::class)
  fun read(dataInput: DataInput): LockInfo {
    dataInput.apply {
      return LockInfo(
        readInt(),
        readLong(),
        readUTF(),
        readUTF()
      )
    }
  }

  private fun trimIfNecessary(inputString: String): String {
    return if (inputString.length > INFORMATION_REGION_DESCR_CHUNK_LIMIT) {
      inputString.substring(0, INFORMATION_REGION_DESCR_CHUNK_LIMIT)
    } else {
      inputString
    }
  }

  companion object {
    const val INFORMATION_REGION_DESCR_CHUNK_LIMIT: Int = 340
  }
}
