package dev.adamko.lokka.internal.serialization

//import java.nio.file.Path
//import kotlin.io.path.Path
//import kotlin.io.path.absolute
//import kotlin.io.path.invariantSeparatorsPathString
//import kotlinx.serialization.KSerializer
//import kotlinx.serialization.builtins.serializer
//import kotlinx.serialization.encoding.Decoder
//import kotlinx.serialization.encoding.Encoder
//
//internal class PathAsStringSerializer : KSerializer<Path> {
//  override val descriptor = String.serializer().descriptor
//  override fun deserialize(decoder: Decoder): Path {
//    return Path(decoder.decodeString())
//  }
//
//  override fun serialize(encoder: Encoder, value: Path) {
//    encoder.encodeString(value.absolute().normalize().invariantSeparatorsPathString)
//  }
//}
