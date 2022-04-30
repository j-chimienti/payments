package payments.models

import com.mathbot.pay.json.PlayJsonSupport
import com.mathbot.pay.lightning.url.CreateInvoiceWithDescriptionHash
import com.mathbot.pay.lightning.{Bolt11, LightningInvoiceStatus, ListInvoice}
import fr.acinq.eclair.MilliSatoshi
import play.api.libs.json.Json

import java.time.Instant

case class LightningInvoiceModel(
                                  id: String,
                                  playerAccountId: String,
                                  bolt11: Bolt11,
                                  description: String,
                                  paymentHash: String,
                                  expires_at: Instant,
                                  created_at: Instant,
                                  status: String,
                                  paid_at: Option[Instant],
                                  pay_index: Option[Long],
                                  msatoshi_received: Option[MilliSatoshi],
                                  bolt12: Option[String] = None,
                                  local_offer_id: Option[String] = None
                                ) {
  lazy val invoiceStatus = LightningInvoiceStatus.withName(status)
}

object LightningInvoiceModel extends PlayJsonSupport {
  def apply(invoice: CreateInvoiceWithDescriptionHash, li: ListInvoice, playerAccountId: String) =
    new LightningInvoiceModel(
      id = li.label,
      playerAccountId = playerAccountId,
      bolt11 = invoice.bolt11,
      description = li.description,
      paymentHash = invoice.payment_hash,
      expires_at = invoice.expires_at,
      created_at = Instant.now(),
      status = li.status.toString,
      pay_index = None,
      paid_at = li.paid_at,
      msatoshi_received = li.amount_msat,
      bolt12 = li.bolt12
    )

  def apply(invoice: ListInvoice, playerAccountId: String): LightningInvoiceModel =
    LightningInvoiceModel(
      id = invoice.label,
      playerAccountId = playerAccountId,
      paymentHash = invoice.payment_hash,
      bolt11 = invoice.bolt11.get,
      pay_index = invoice.pay_index,
      description = invoice.description,
      expires_at =  invoice.expires_at,
      created_at = Instant.now(),
      status = invoice.status.toString,
      paid_at = invoice.paid_at,
      msatoshi_received = invoice.amount_msat
    )


  implicit lazy val formatLightningInvoiceModel = Json.format[LightningInvoiceModel]
}
