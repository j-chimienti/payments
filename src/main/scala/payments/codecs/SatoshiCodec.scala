package payments.codecs

import com.mathbot.pay.bitcoin.Satoshi
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

class SatoshiCodec extends Codec[Satoshi] {
  override def decode(reader: BsonReader, decoderContext: DecoderContext): Satoshi = Satoshi(reader.readInt64)

  override def encode(writer: BsonWriter, value: Satoshi, encoderContext: EncoderContext): Unit =
    writer.writeInt64(value.toLong)

  override def getEncoderClass: Class[Satoshi] = classOf[Satoshi]
}
