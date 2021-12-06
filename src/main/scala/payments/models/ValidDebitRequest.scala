package payments.models

import com.mathbot.pay.bitcoin.{MilliSatoshi, Satoshi}
import com.mathbot.pay.lightning.{Bolt11, Pay}
import play.api.libs.json.Json

import java.time.Instant

object ValidDebitRequest {
  implicit val formatValidDebitRequest = Json.format[ValidDebitRequest]
}
case class ValidDebitRequest(pay: Pay,
                             playerAccountId: String,
                             msatoshi: MilliSatoshi,
                             availableBalance: MilliSatoshi,
                             fee: Satoshi,
                             createdAt: Instant) {
  override def toString: String = Json.toJson(this).toString()

}
