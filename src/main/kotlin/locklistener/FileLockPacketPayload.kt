/*
 * Copyright 2018 the original author or authors.
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
package locklistener

import java.io.*

class FileLockPacketPayload private constructor(
  @JvmField val lockId: Long,
  @JvmField val type: FileLockPacketType,
) {
  companion object {
    const val MAX_BYTES: Int = 1 + 8 + 1 // protocolVersion + lockId + type
    private const val PROTOCOL_VERSION: Byte = 1
    private val TYPES: List<FileLockPacketType> = FileLockPacketType.entries.toList()

    @Throws(IOException::class)
    fun encode(lockId: Long, type: FileLockPacketType): ByteArray {
      val out = ByteArrayOutputStream()
      val dataOutput = DataOutputStream(out)
      try {
        dataOutput.writeByte(PROTOCOL_VERSION.toInt())
        dataOutput.writeLong(lockId)
        dataOutput.writeByte(type.ordinal)
        dataOutput.flush()
      } catch (e: IOException) {
        throw IOException("Failed to encode lockId $lockId and type $type", e)
      }
      return out.toByteArray()
    }

    @Throws(IOException::class)
    fun decode(bytes: ByteArray, length: Int): FileLockPacketPayload {
      try {
        val dataInput = DataInputStream(ByteArrayInputStream(bytes))
        val version = dataInput.readByte()
        require(version == PROTOCOL_VERSION) {
          "Unexpected protocol version $version received in lock contention notification message"
        }
        val lockId = dataInput.readLong()
        val type = readType(dataInput, length)
        return FileLockPacketPayload(lockId, type)
      } catch (e: IOException) {
        // This should never happen as we are reading from a byte array
        throw IOException(e)
      }
    }

    @Throws(IOException::class)
    private fun readType(dataInput: DataInputStream, length: Int): FileLockPacketType {
      if (length < MAX_BYTES) {
        return FileLockPacketType.UNKNOWN
      }
      try {
        val ordinal = dataInput.readByte().toInt()
        if (ordinal < TYPES.size) {
          return TYPES[ordinal]
        }
      } catch (ignore: EOFException) {
        // old versions don't send a type
      }
      return FileLockPacketType.UNKNOWN
    }
  }
}
