package payments.payments.models

import akka.actor.ActorRef
import com.mathbot.pay.lightning.Bolt11

/**
 * Debit request from the player. Needs to be validated before payment
 * @param id
 * @param playerAccountId
 * @param bolt11
 * @param replyTo
 */
case class DebitRequest(id: SecureIdentifier, playerAccountId: SecureIdentifier, bolt11: Bolt11, replyTo: ActorRef)
