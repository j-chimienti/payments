package payments.payments.models

import akka.actor.ActorRef
import com.mathbot.pay.bitcoin.Satoshi

case class CreditRequest(satoshi: Satoshi, playerAccountId: SecureIdentifier, replyTo: ActorRef)
