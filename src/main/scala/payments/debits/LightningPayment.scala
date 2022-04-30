package payments.debits

import com.mathbot.pay.lightning.{Bolt11, ListPay, PayService, PayStatus}
import fr.acinq.eclair.MilliSatoshi
import play.api.libs.json._

import java.time.Instant

object LightningPayment {
  def pending(p: PayService.PlayerPayment__IN): LightningPayment = {
    val n = Instant.now()
    new LightningPayment(
      tokenId = p.playerId,
      label = Some(p.label),
      status = PayStatus.pending.toString,
      amount_msat = p.bolt11.milliSatoshi,
      createdAt = n,
      bolt11 = p.bolt11,
      amount_sent_msat = p.bolt11.milliSatoshi,
      preimage = None,
      payment_hash = Some(p.bolt11.invoice.paymentHash.toString),
      updatedAt = n
    )
  }

  lazy implicit val formatDebit: OFormat[LightningPayment] = Json.format[LightningPayment]

  def apply(listPay: ListPay, tokenId: String): LightningPayment = new LightningPayment(
    tokenId = tokenId,
    label = listPay.label,
    status = listPay.status.toString,
    amount_msat = listPay.amount_msat.get,
    createdAt = listPay.created_at,
    bolt11 = listPay.bolt11.get,
    amount_sent_msat = listPay.amount_sent_msat,
    preimage = listPay.preimage,
    payment_hash = listPay.payment_hash,
    updatedAt = Instant.now()
  )

}

/**
 *
 * copy of [[com.mathbot.pay.lightning.ListPay]] for use to insert into db and add player info
 * @param tokenId PlayerAccount.tokenId
 * @param label self generated id
 * @param status [[PayStatus]] in string format for mongo serialization issues w/ enums
 * @param amount_msat to debit player
 * @param createdAt
 * @param bolt11 of sent payment to player
 * @param amount_sent_msat total sent for invoice with fees eg 1001msat
 * @param preimage of payment
 * @param payment_hash of payment
 */
case class LightningPayment(
                             tokenId: String,
                             status: String,
                             label: Option[String],
                             amount_msat: MilliSatoshi,
                             createdAt: Instant,
                             bolt11: Bolt11,
                             amount_sent_msat: MilliSatoshi,
                             preimage: Option[String],
                             payment_hash: Option[String],
                             updatedAt: Instant
                           ) {
  val payStatus: PayStatus.Value = PayStatus.withName(status)
  val msatoshi: MilliSatoshi = amount_msat
  val isCompleteOrPending: Boolean = payStatus == PayStatus.complete || payStatus == PayStatus.pending
  val bolt11Invoice = bolt11.invoice
  val paymentHash = bolt11Invoice.paymentHash.toString
  override def toString: String = Json.toJson(this).toString()
}
