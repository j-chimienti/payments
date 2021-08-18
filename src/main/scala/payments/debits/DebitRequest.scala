package payments.debits

import akka.actor.ActorRef
import com.mathbot.pay.lightning.Bolt11
import payments.models.SecureIdentifier

/**
 * Debit request from the player. Needs to be validated before payment
 *
 * @param id
 * @param playerAccountId
 * @param bolt11
 * @param replyTo
 */
case class DebitRequest(playerAccountId: SecureIdentifier, bolt11: Bolt11, replyTo: Option[ActorRef])
