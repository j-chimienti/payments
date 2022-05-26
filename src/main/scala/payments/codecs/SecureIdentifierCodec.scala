package payments.codecs

import com.mathbot.pay.SecureIdentifier
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

class SecureIdentifierCodec extends Codec[SecureIdentifier] {
  override def decode(reader: BsonReader, decoderContext: DecoderContext): SecureIdentifier =
    SecureIdentifier(reader.readString())

  override def encode(writer: BsonWriter, value: SecureIdentifier, encoderContext: EncoderContext): Unit =
    writer.writeString(value.toString)

  override def getEncoderClass: Class[SecureIdentifier] = classOf[SecureIdentifier]
}
