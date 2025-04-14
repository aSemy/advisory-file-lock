@file:OptIn(ExperimentalSerializationApi::class)

package serialization

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule


internal val kxsBinary: KxsBinary = KxsBinary()

/** [`https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#efficient-binary-format`](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#efficient-binary-format) */
class KxsBinary(
  override val serializersModule: SerializersModule = EmptySerializersModule()
) : BinaryFormat {

  override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val output = DataOutputStream(outputStream)
    val encoder = KxsDataOutputEncoder(output, serializersModule)
    encoder.encodeSerializableValue(serializer, value)
    return outputStream.toByteArray()
  }

  override fun <T> decodeFromByteArray(
    deserializer: DeserializationStrategy<T>,
    bytes: ByteArray
  ): T {
    val inputStream = bytes.inputStream()
    val input = DataInputStream(inputStream)
    val decoder = KxsDataInputDecoder(input, serializersModule)
    return decoder.decodeSerializableValue(deserializer)
  }
}

private val byteArraySerializer = ByteArraySerializer()


private class KxsDataOutputEncoder(
  private val output: DataOutputStream,
  override val serializersModule: SerializersModule = EmptySerializersModule(),
) : AbstractEncoder() {

  override fun encodeBoolean(value: Boolean): Unit = output.writeBoolean(value)
  override fun encodeByte(value: Byte): Unit = output.writeByte(value.toInt())
  override fun encodeChar(value: Char): Unit = output.writeChar(value.code)
  override fun encodeDouble(value: Double): Unit = output.writeDouble(value)
  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit = output.writeInt(index)
  override fun encodeFloat(value: Float): Unit = output.writeFloat(value)
  override fun encodeInt(value: Int): Unit = output.writeInt(value)
  override fun encodeLong(value: Long): Unit = output.writeLong(value)
  override fun encodeShort(value: Short): Unit = output.writeShort(value.toInt())
  override fun encodeString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    encodeCompactSize(bytes.size)
    output.write(bytes)
  }

  override fun beginCollection(
    descriptor: SerialDescriptor,
    collectionSize: Int
  ): CompositeEncoder {
    encodeCompactSize(collectionSize)
    return this
  }

  override fun encodeNull(): Unit = encodeBoolean(false)
  override fun encodeNotNullMark(): Unit = encodeBoolean(true)

  override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
    when (serializer.descriptor) {
      byteArraySerializer.descriptor ->
        encodeByteArray(value as ByteArray)

      else                           ->
        super.encodeSerializableValue(serializer, value)
    }
  }

  private fun encodeByteArray(bytes: ByteArray) {
    encodeCompactSize(bytes.size)
    output.write(bytes)
  }

  private fun encodeCompactSize(value: Int) {
    if (value < 0xff) {
      output.writeByte(value)
    } else {
      output.writeByte(0xff)
      output.writeInt(value)
    }
  }
}


private class KxsDataInputDecoder(
  private val input: DataInputStream,
  override val serializersModule: SerializersModule = EmptySerializersModule(),
  private var elementsCount: Int = 0,
) : AbstractDecoder() {

  private var elementIndex = 0

  override fun decodeBoolean(): Boolean = input.readBoolean()
  override fun decodeByte(): Byte = input.readByte()
  override fun decodeChar(): Char = input.readChar()
  override fun decodeDouble(): Double = input.readDouble()
  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()
  override fun decodeFloat(): Float = input.readFloat()
  override fun decodeInt(): Int = input.readInt()
  override fun decodeLong(): Long = input.readLong()
  override fun decodeShort(): Short = input.readShort()
  override fun decodeString(): String {
    val size = decodeCompactSize()
    val bytes = ByteArray(size)
    input.readFully(bytes)
    return bytes.toString(Charsets.UTF_8)
  }

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
    return elementIndex++
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
    KxsDataInputDecoder(input, serializersModule, descriptor.elementsCount)

  override fun decodeSequentially(): Boolean = true

  override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
    decodeCompactSize().also { elementsCount = it }

  override fun decodeNotNullMark(): Boolean = decodeBoolean()

  override fun <T> decodeSerializableValue(
    deserializer: DeserializationStrategy<T>,
    previousValue: T?,
  ): T {
    return when (deserializer.descriptor) {
      byteArraySerializer.descriptor -> {
        val result = decodeByteArray()
        @Suppress("UNCHECKED_CAST")
        return result as T
      }

      else                           ->
        super.decodeSerializableValue(deserializer, previousValue)
    }
  }

  private fun decodeByteArray(): ByteArray {
    val size = decodeCompactSize()
    return input.readNBytes(size)
  }

  private fun decodeCompactSize(): Int {
    val byte = input.readByte().toInt() and 0xff
    return if (byte < 0xff) byte else input.readInt()
  }
}
