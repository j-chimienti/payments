package payments.models

import com.mathbot.pay.lightning.Pay
import fr.acinq.eclair.MilliSatoshi
import play.api.libs.json.Json

import java.time.Instant

object ValidDebitRequest {
  implicit val formatValidDebitRequest = Json.format[ValidDebitRequest]
}
case class ValidDebitRequest(pay: Pay,
                             playerAccountId: String,
                             msatoshi: MilliSatoshi,
                             availableBalance: MilliSatoshi,
                             feeMsat: MilliSatoshi,
                             createdAt: Instant) {
  override def toString: String = Json.toJson(this).toString()

}
