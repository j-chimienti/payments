package payments.debits

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.Bolt11
import com.mathbot.pay.lightning.PayStatus.PayStatus
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.{equal, gte}
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model._
import payments.models.SecureIdentifier
import payments.utils.MongoCollectionTrait

import java.time.Instant
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object DebitsDAO {
  val collectionName = "debits"

}
class DebitsDAO(val collection: MongoCollection[Debit])(implicit
                                                        ec: ExecutionContext)
    extends MongoCollectionTrait[Debit] {

  def findByStatus(pending: PayStatus) = collection.find(equal(nameOf[Debit](_.status), pending.toString)).toFuture()

  def find(playerAccountId: SecureIdentifier): Future[Seq[Debit]] =
    collection.find(equal(nameOf[Debit](_.playerAccountId), playerAccountId.toString)).toFuture

  def find(bolt11: Bolt11): Future[Option[Debit]] = findBolt11(bolt11.bolt11)

  def findBolt11(bolt11: Bolt11): Future[Option[Debit]] = findBolt11(bolt11.bolt11)

  def updateStatus(bolt11: Bolt11, status: PayStatus): Future[Option[Debit]] =
    collection
      .findOneAndUpdate(
        equal(nameOf[Debit](_.bolt11), bolt11.bolt11),
        combine(
          set(nameOf[Debit](_.status), status.toString)
        ),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      )
      .toFutureOption

  def findBolt11(bolt11: String): Future[Option[Debit]] =
    collection.find(equal(nameOf[Debit](_.bolt11), bolt11)).headOption

  def findWithin(playerAccountId: SecureIdentifier, time: FiniteDuration): Future[Seq[Debit]] =
    collection
      .find(
        Filters.and(
          equal(nameOf[Debit](_.playerAccountId), playerAccountId.toString),
          gte(nameOf[Debit](_.createdAt), Instant.now().minusMillis(time.toMillis))
        )
      )
      .toFuture()

  def findWithin(timeSpan: FiniteDuration = 1 hour): Future[Seq[Debit]] =
    collection.find(gte(nameOf[Debit](_.createdAt), Instant.now().minusSeconds(timeSpan.toSeconds))).toFuture()

  {
    collection.createIndex(Indexes.ascending(nameOf[Debit](_.bolt11)), IndexOptions().unique(true)).toFutureOption()
    collection
      .createIndex(Indexes.ascending(nameOf[Debit](_.playerAccountId), nameOf[Debit](_.status)))
      .toFutureOption()
  }

}
