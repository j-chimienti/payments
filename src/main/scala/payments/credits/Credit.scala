package payments.credits

import com.mathbot.pay.bitcoin.Satoshi
import com.mathbot.pay.lightning.Bolt11
import payments.models.SecureIdentifier
import play.api.libs.json._

import java.time.Instant

case class Credit(
    label: String, // ln label
    playerAccountId: String,
    satoshi: Satoshi,
    bolt11: Bolt11,
    created_at: Instant
) {

  override def toString: String =
    Json.toJson(this).toString()
}

object Credit {

  implicit val formatCredit: Format[Credit] = Json.format[Credit]

  def apply(invoiceLabel: String, bolt11: Bolt11, playerAccountId: String): Credit =
    Credit(
      label = invoiceLabel,
      playerAccountId = playerAccountId,
      satoshi = bolt11.milliSatoshi.toSatoshi,
      bolt11 = bolt11,
      created_at = Instant.now()
    )

}
