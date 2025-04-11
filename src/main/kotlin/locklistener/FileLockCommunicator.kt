/*
 * Copyright 2025 the original author or authors.
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

import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketAddress
import java.util.*

//@NullMarked
interface FileLockCommunicator {
  fun pingOwner(address: InetAddress?, ownerPort: Int, lockId: Long, displayName: String?): Boolean

  @Throws(IOException::class)
  fun receive(): Optional<DatagramPacket>

  fun decode(receivedPacket: DatagramPacket): FileLockPacketPayload

  fun confirmUnlockRequest(requesterAddress: SocketAddress, lockId: Long)

  fun confirmLockRelease(requesterAddresses: Set<SocketAddress>, lockId: Long)

  fun stop()

  val port: Int
}
