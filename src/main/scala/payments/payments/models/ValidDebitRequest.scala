package payments.payments.models

import java.time.Instant
import com.mathbot.pay.bitcoin.{MilliSatoshi, Satoshi}
import com.mathbot.pay.lightning.PayStatus.PayStatus
import com.mathbot.pay.lightning.{Bolt11, PayStatus}
import play.api.libs.json.Json

object ValidDebitRequest {
  implicit val formatValidDebitRequest = Json.format[ValidDebitRequest]
}
case class ValidDebitRequest(bolt11: Bolt11,
                             playerAccountId: SecureIdentifier,
                             msatoshi: MilliSatoshi,
                             availableBalance: MilliSatoshi,
                             fee: Satoshi,
                             createdAt: Instant) {
  override def toString: String = Json.toJson(this).toString()

}
