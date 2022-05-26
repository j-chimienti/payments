package payments.debits

import akka.actor.ActorRef
import com.mathbot.pay.lightning.Bolt11

/**
 * Debit request from the player. Needs to be validated before payment
 *
 * @param id
 * @param playerAccountId
 * @param bolt11
 * @param replyTo
 */
case class DebitRequest(playerAccountId: String, bolt11: Bolt11, replyTo: Option[ActorRef])
