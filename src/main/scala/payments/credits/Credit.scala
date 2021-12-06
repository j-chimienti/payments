package payments.credits

import com.mathbot.pay.bitcoin.{MilliSatoshi, Satoshi}
import com.mathbot.pay.lightning.{Bolt11, ListInvoice}
import payments.lightninginvoices.LightningInvoiceModel
import play.api.libs.json._

import java.time.Instant

case class Credit(
    label: String, // ln label
    playerAccountId: String,
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
  def apply(invoice: LightningInvoiceModel, playerAccountId: String): Credit =   new Credit(
    label = invoice.id,
    playerAccountId = playerAccountId,
    paymentHash = invoice.paymentHash,
    satoshi = invoice.bolt11.milliSatoshi.toSatoshi,
    bolt11 = Some(invoice.bolt11),
    bolt12 = invoice.bolt12,
    created_at = Instant.now()
  )


  implicit val formatCredit: Format[Credit] = Json.format[Credit]

  def apply(invoice: ListInvoice, playerAccountId: String): Credit =
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
