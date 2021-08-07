package payments.debits

import com.mathbot.pay.bitcoin.Satoshi
import com.mathbot.pay.lightning.{Bolt11, ListPay, PayStatus}
import payments.models.{SecureIdentifier, ValidDebitRequest}
import play.api.libs.json._

import java.time.Instant

case class Debit(
    status: String,
    playerAccountId: SecureIdentifier,
    bolt11: Bolt11,
    createdAt: Instant,
    satoshi: Satoshi
) {
  lazy val debitStatus = PayStatus.withName(status)
  override def toString: String = Json.toJson(this).toString()

}

object Debit {

  implicit val formatDebit: Format[Debit] = Json.format[Debit]
  @throws(classOf[IllegalArgumentException])
  def apply(payResponse: ListPay, debitRequest: ValidDebitRequest): Debit = {
    require(
      payResponse.status == PayStatus.complete || payResponse.status == PayStatus.failed,
      s"Invalid debit status ${payResponse.status} expecting ${PayStatus.complete} or ${PayStatus.failed}"
    )
    require(payResponse.bolt11.isDefined, s"Invalid list pay -- missing bolt11 $payResponse")
    require(payResponse.bolt11.get == debitRequest.bolt11, s"Invalid bolt11 matching $payResponse $debitRequest")
    val paymentHash =
      payResponse.payment_hash.getOrElse(throw new IllegalArgumentException("Invalid listpay missing payment_hash "))
    Debit(
      status = payResponse.status.toString,
      playerAccountId = debitRequest.playerAccountId,
      bolt11 = debitRequest.bolt11,
      createdAt = Instant.now(),
      satoshi = debitRequest.bolt11.milliSatoshi.toSatoshi
    )
  }

  def apply(debitRequest: ValidDebitRequest): Debit =
    Debit(
      status = PayStatus.pending.toString,
      playerAccountId = debitRequest.playerAccountId,
      bolt11 = debitRequest.bolt11,
      createdAt = Instant.now(),
      satoshi = debitRequest.bolt11.milliSatoshi.toSatoshi
    )

}
