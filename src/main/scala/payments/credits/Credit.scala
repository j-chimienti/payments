package payments.credits

import com.mathbot.pay.bitcoin.MilliSatoshi
import com.mathbot.pay.lightning.{Bolt11, ListInvoice}
import payments.lightninginvoices.LightningInvoiceModel
import play.api.libs.json._

import java.time.Instant

case class Credit(
    label: String, // ln label
    playerAccountId: String,
    paymentHash: String,
    bolt11: Option[Bolt11],
    bolt12: Option[String],
    created_at: Instant,
    description: String,
    paid_at: Instant,
    pay_index: Long,
    local_offer_id: Option[String],
    msatoshi_received: MilliSatoshi,
) {

  override def toString: String =
    Json.toJson(this).toString()

}

object Credit {
  def apply(invoice: LightningInvoiceModel, playerAccountId: String): Credit =   new Credit(
    label = invoice.id,
    playerAccountId = playerAccountId,
    paymentHash = invoice.paymentHash,
    bolt11 = Some(invoice.bolt11),
    bolt12 = invoice.bolt12,
    created_at = Instant.now(),
    msatoshi_received = invoice.msatoshi_received.get,
    paid_at = invoice.paid_at.get,
    pay_index = invoice.pay_index.get,
    local_offer_id = invoice.local_offer_id,
    description = invoice.description
  )


  implicit val formatCredit: Format[Credit] = Json.format[Credit]

  def apply(invoice: ListInvoice, playerAccountId: String): Credit =
    new Credit(
      label = invoice.label,
      playerAccountId = playerAccountId,
      paymentHash = invoice.payment_hash,
      bolt11 = invoice.bolt11,
      bolt12 = invoice.bolt12,
      created_at = Instant.now(), // todo: read from invoice
      msatoshi_received = invoice.msatoshi_received.get,
      paid_at = invoice.paid_at.get,
      pay_index = invoice.pay_index.get,
      local_offer_id = invoice.local_offer_id,
      description = invoice.description
    )

}
