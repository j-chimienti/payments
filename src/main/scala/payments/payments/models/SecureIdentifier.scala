package payments.payments.models

import java.security.SecureRandom
import java.util.Base64

import play.api.libs.json.{Format, JsError, JsSuccess}

class SecureIdentifier private (aId: Seq[Byte]) {
  private val id: Seq[Byte] = aId

  def toByteArray: Array[Byte] = id.toArray

  override def toString: String =
    SecureIdentifier.encoder.encodeToString(id.toArray)

  override def equals(obj: scala.Any): Boolean =
    super.equals(obj) || (obj match {
      case that: SecureIdentifier => that.id.equals(this.id)
      case _ => false
    })

  override def hashCode(): Int = this.id.hashCode()

  // Adds more random bytes to an identifier, good for creating a salt from an existing id
  def extend(size: Int): SecureIdentifier = {
    val e = SecureIdentifier(size)
    new SecureIdentifier(id ++ e.id)
  }

  def trimExtension(size: Int): SecureIdentifier = {
    new SecureIdentifier(id.take(id.length - size))
  }
}

object SecureIdentifier {

  private val encoder = Base64.getUrlEncoder
  private val decoder = Base64.getUrlDecoder
  private val random = new SecureRandom

  implicit val secureIdentifierPlayFormatter: Format[SecureIdentifier] =
    new Format[SecureIdentifier] {
      import play.api.libs.json.{JsResult, JsString, JsValue}
      override def writes(o: SecureIdentifier): JsValue = JsString(o.toString)

      override def reads(json: JsValue): JsResult[SecureIdentifier] =
        json match {
          case JsString(base64String) =>
            JsSuccess(SecureIdentifier(base64String))
          case _ => JsError("Invalid SecureIdentifier")
        }
    }

  def apply(aId: String): SecureIdentifier =
    new SecureIdentifier(decoder.decode(aId).toSeq)

  def apply(size: Int): SecureIdentifier = {
    val buffer = Array.fill[Byte](size)(0)
    random.nextBytes(buffer)
    new SecureIdentifier(buffer.toSeq)
  }

  def empty = SecureIdentifier(0)

  def fromStringOpt(sessionId: String): Option[SecureIdentifier] = {
    try {
      Some(SecureIdentifier(sessionId))
    } catch {
      case _: IllegalArgumentException =>
        None
    }
  }

}
