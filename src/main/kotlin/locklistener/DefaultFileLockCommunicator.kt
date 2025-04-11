/*
 * Copyright 2011 the original author or authors.
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
import java.net.*
import java.util.*

//@NullMarked
class DefaultFileLockCommunicator(
  inetAddressProvider: InetAddressProvider
) : FileLockCommunicator {
  private val socket: DatagramSocket =
    try {
      DatagramSocket(0, inetAddressProvider.wildcardBindingAddress)
    } catch (e: SocketException) {
      throw e
    }

//  init {
//    try {
//      socket = DatagramSocket(0, inetAddressProvider.wildcardBindingAddress)
//    } catch (e: SocketException) {
//      throw e
//    }
//  }

  override fun pingOwner(address: InetAddress?, ownerPort: Int, lockId: Long, displayName: String?): Boolean {
    var pingSentSuccessfully = false
    val bytesToSend = FileLockPacketPayload.encode(lockId, FileLockPacketType.UNLOCK_REQUEST)
    try {
      socket!!.send(DatagramPacket(bytesToSend, bytesToSend.size, address, ownerPort))
      pingSentSuccessfully = true
    } catch (e: IOException) {
      println(
        "Failed attempt to ping owner of lock for $displayName (lock id: $lockId, port: $ownerPort, address: $address)",
      )
    }
    return pingSentSuccessfully
  }

  @Throws(IOException::class)
  override fun receive(): Optional<DatagramPacket> {
    try {
      val bytes = ByteArray(FileLockPacketPayload.MAX_BYTES)
      val packet = DatagramPacket(bytes, bytes.size)
      socket.receive(packet)
      return Optional.of(packet)
    } catch (e: IOException) {
      // Socket was shutdown while waiting to receive message
      if (socket.isClosed) {
        return Optional.empty()
      }
      throw e
    }
  }

  override fun decode(receivedPacket: DatagramPacket): FileLockPacketPayload {
    return FileLockPacketPayload.decode(receivedPacket.data, receivedPacket.length)
  }

  override fun confirmUnlockRequest(requesterAddress: SocketAddress, lockId: Long) {
    val bytes = FileLockPacketPayload.encode(lockId, FileLockPacketType.UNLOCK_REQUEST_CONFIRMATION)
    val packet = DatagramPacket(bytes, bytes.size)
    packet.socketAddress = requesterAddress
    println("Confirming unlock request to Gradle process at port ${packet.port} for lock with id $lockId.")
    try {
      socket!!.send(packet)
    } catch (e: IOException) {
      println(
        "Failed to confirm unlock request to Gradle process at port ${packet.port} for lock with id $lockId.",
      )
    }
  }

  override fun confirmLockRelease(requesterAddresses: Set<SocketAddress>, lockId: Long) {
    val bytes = FileLockPacketPayload.encode(lockId, FileLockPacketType.LOCK_RELEASE_CONFIRMATION)
    for (requesterAddress in requesterAddresses) {
      val packet = DatagramPacket(bytes, bytes.size)
      packet.socketAddress = requesterAddress
      println("Confirming lock release to Gradle process at port ${packet.port} for lock with id ${lockId}.")
      try {
        socket!!.send(packet)
      } catch (e: IOException) {
        println("Failed to confirm lock release to Gradle process at port ${packet.port} for lock with id ${lockId}.")
      }
    }
  }

  override fun stop() {
    socket.close()
  }

  override val port: Int
    get() = socket.localPort

  companion object {
//    private val LOGGER: Logger = LoggerFactory.getLogger(DefaultFileLockCommunicator::class.java)
  }
}
