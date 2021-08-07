package payments.payments.models

import java.time.Instant

import com.mathbot.pay.bitcoin.Satoshi
import com.mathbot.pay.json.PlayJsonSupport
import com.mathbot.pay.lightning.Bolt11
import play.api.libs.json._

case class Credit(
    label: String, // ln label
    playerAccountId: SecureIdentifier,
    satoshi: Satoshi,
    bolt11: Bolt11,
    created_at: Instant
) {

  override def toString: String =
    Json.toJson(this).toString()
}

object Credit extends PlayJsonSupport {

  implicit val formatCredit: Format[Credit] = Json.format[Credit]

  def apply(invoiceLabel: String, bolt11: Bolt11, playerAccountId: SecureIdentifier): Credit =
    Credit(
      label = invoiceLabel,
      playerAccountId = playerAccountId,
      satoshi = bolt11.milliSatoshi.toSatoshi,
      bolt11 = bolt11,
      created_at = Instant.now()
    )

}
