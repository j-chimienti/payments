package payments.credits

import akka.actor.ActorRef
import com.mathbot.pay.bitcoin.Satoshi
import payments.models.SecureIdentifier

case class CreditRequest(satoshi: Satoshi, playerAccountId: String, replyTo: ActorRef)
