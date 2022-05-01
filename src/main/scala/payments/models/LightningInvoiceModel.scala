package payments.models

import com.mathbot.pay.json.PlayJsonSupport
import com.mathbot.pay.lightning.url.CreateInvoiceWithDescriptionHash
import com.mathbot.pay.lightning.{Bolt11, LightningInvoiceStatus, ListInvoice}
import fr.acinq.eclair.MilliSatoshi
import play.api.libs.json.Json

import java.time.Instant

/**
 * DAO for [[ListInvoice]]. Created since there were issues storing enums in mongo
 * @param metadata store some data associated with the invoice
 * @param label
 * @param bolt11
 * @param description
 * @param payment_hash
 * @param expires_at
 * @param created_at
 * @param status
 * @param paid_at
 * @param pay_index
 * @param msatoshi_received
 */
case class LightningInvoiceModel(
    metadata: String,
    label: String,
    bolt11: Bolt11,
    description: String,
    payment_hash: String,
    expires_at: Instant,
    created_at: Instant,
    status: String,
    paid_at: Option[Instant],
    pay_index: Option[Long],
    amount_msat: Option[MilliSatoshi],
    amount_received_msat: Option[MilliSatoshi]
) {
  lazy val invoiceStatus = LightningInvoiceStatus.withName(status)
  lazy val payStatus = invoiceStatus
  lazy val listInvoice = ListInvoice(
    label = label,
    bolt11 = Some(bolt11),
    payment_hash = payment_hash,
    amount_msat = amount_msat,
    amount_received_msat = amount_received_msat,
    status = payStatus,
    pay_index = pay_index,
    paid_at = paid_at,
    description = description,
    expires_at = expires_at,
    bolt12 = None,
    local_offer_id = None,
    payer_note = None,
    payment_preimage = None,
  )
}

object LightningInvoiceModel extends PlayJsonSupport {
  def apply(invoice: CreateInvoiceWithDescriptionHash, li: ListInvoice, playerAccountId: String) =
    new LightningInvoiceModel(
      metadata = playerAccountId,
      bolt11 = invoice.bolt11,
      description = li.description,
      payment_hash = invoice.payment_hash,
      expires_at = invoice.expires_at,
      created_at = Instant.now(),
      status = li.status.toString,
      pay_index = None,
      paid_at = li.paid_at,
      label = li.label,
      amount_received_msat = li.amount_received_msat,
      amount_msat = li.amount_msat
    )

  def apply(invoice: ListInvoice, playerAccountId: String): LightningInvoiceModel =
    LightningInvoiceModel(
      metadata = playerAccountId,
      payment_hash = invoice.payment_hash,
      bolt11 = invoice.bolt11.get,
      pay_index = invoice.pay_index,
      description = invoice.description,
      expires_at = invoice.expires_at,
      created_at = Instant.now(),
      status = invoice.status.toString,
      paid_at = invoice.paid_at,
      amount_msat = invoice.amount_msat,
      label = invoice.label,
      amount_received_msat = invoice.amount_received_msat
    )

  implicit lazy val formatLightningInvoiceModel = Json.format[LightningInvoiceModel]
}
