package payments.codecs

import com.mathbot.pay.lightning.Bolt11
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

class Bolt11Codec extends Codec[Bolt11] {
  override def encode(writer: BsonWriter, value: Bolt11, encoderContext: EncoderContext): Unit =
    writer.writeString(value.bolt11)

  override def getEncoderClass: Class[Bolt11] = classOf[Bolt11]

  override def decode(reader: BsonReader, decoderContext: DecoderContext): Bolt11 = Bolt11(reader.readString)
}
