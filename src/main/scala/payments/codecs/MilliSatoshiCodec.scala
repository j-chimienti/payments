package payments.codecs

import fr.acinq.eclair.MilliSatoshi
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

class MilliSatoshiCodec extends Codec[MilliSatoshi] {
  override def decode(reader: BsonReader, decoderContext: DecoderContext): MilliSatoshi = MilliSatoshi(reader.readInt64)

  override def encode(writer: BsonWriter, value: MilliSatoshi, encoderContext: EncoderContext): Unit =
    writer.writeInt64(value.toLong)

  override def getEncoderClass: Class[MilliSatoshi] = classOf[MilliSatoshi]
}
