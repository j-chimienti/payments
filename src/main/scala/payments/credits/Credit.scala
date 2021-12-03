package payments.credits

import com.mathbot.pay.bitcoin.{MilliSatoshi, Satoshi}
import com.mathbot.pay.lightning.{Bolt11, ListInvoice}
import payments.models.SecureIdentifier
import play.api.libs.json._

import java.time.Instant

case class Credit(
    label: String, // ln label
    playerAccountId: SecureIdentifier,
    paymentHash: String,
    satoshi: Satoshi,
    bolt11: Option[Bolt11],
    bolt12: Option[String],
    created_at: Instant
) {

  override def toString: String =
    Json.toJson(this).toString()
}

object Credit {

  implicit val formatCredit: Format[Credit] = Json.format[Credit]

  def apply(invoice: ListInvoice, playerAccountId: SecureIdentifier): Credit =
    new Credit(
      label = invoice.label,
      playerAccountId = playerAccountId,
      paymentHash = invoice.payment_hash,
      satoshi = invoice.msatoshi.getOrElse(MilliSatoshi(0)).toSatoshi,
      bolt11 = invoice.bolt11,
      bolt12 = invoice.bolt12,
      created_at = Instant.now()
    )

}
