package payments.debits

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning
import com.mathbot.pay.lightning.PayStatus.PayStatus
import com.mathbot.pay.lightning.{Bolt11, ListPay, Payment}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.{equal, gte, or}
import org.mongodb.scala.model.Updates.{combine, currentDate, set}
import org.mongodb.scala.model._
import payments.models.ValidDebitRequest
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
  def updateStatus(bolt11: Bolt11, failed: lightning.PayStatus.Value) =
    collection
      .findOneAndUpdate(
        equal(nameOf[Debit](_.bolt11), bolt11.bolt11),
        combine(
          set(nameOf[Debit](_.status), failed),
          currentDate(nameOf[Debit](_.modifiedAt))
        ),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      )
      .toFutureOption

  def updateOne(pay: Payment, dr: ValidDebitRequest) =
    collection
      .findOneAndUpdate(
        equal(nameOf[Debit](_.bolt11), dr.pay.bolt11),
        combine(
          set(nameOf[Debit](_.status), pay.status.toString),
          set(nameOf[Debit](_.paymentHash), pay.payment_hash),
          currentDate(nameOf[Debit](_.modifiedAt))
        ),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      )
      .toFutureOption

  def find() = collection.find().toFuture()
  def findByStatus(status: PayStatus) = collection.find(equal(nameOf[Debit](_.status), status.toString)).toFuture()

  def find(playerAccountId: String): Future[Seq[Debit]] =
    collection.find(equal(nameOf[Debit](_.playerAccountId), playerAccountId)).toFuture

  def findByLabel(label: String): Future[Seq[Debit]] =
    collection.find(equal(nameOf[Debit](_.label), label)).toFuture

  def find(bolt11: Bolt11): Future[Option[Debit]] = findBolt11(bolt11)

  def updateOne(pay: ListPay): Future[Option[Debit]] =
    collection
      .findOneAndUpdate(
        or(
          equal(nameOf[Debit](_.paymentHash), pay.payment_hash.orNull),
          equal(nameOf[Debit](_.bolt11), pay.bolt11.map(_.bolt11).orNull),
          equal(nameOf[Debit](_.label), pay.label.orNull)
        ),
        combine(
          set(nameOf[Debit](_.status), pay.status.toString),
          set(nameOf[Debit](_.paymentHash), pay.payment_hash.orNull),
          currentDate(nameOf[Debit](_.modifiedAt))
        ),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      )
      .toFutureOption

  def findBolt11(bolt11: Bolt11): Future[Option[Debit]] =
    collection.find(equal(nameOf[Debit](_.bolt11), bolt11.bolt11)).headOption

  def findWithin(playerAccountId: String, time: FiniteDuration): Future[Seq[Debit]] =
    collection
      .find(
        Filters.and(
          equal(nameOf[Debit](_.playerAccountId), playerAccountId),
          gte(nameOf[Debit](_.createdAt), Instant.now().minusMillis(time.toMillis))
        )
      )
      .toFuture()

  def findWithin(timeSpan: FiniteDuration = 1 hour): Future[Seq[Debit]] =
    collection.find(gte(nameOf[Debit](_.createdAt), Instant.now().minusSeconds(timeSpan.toSeconds))).toFuture()

  {
    collection.createIndex(Indexes.ascending(nameOf[Debit](_.bolt11)), IndexOptions().unique(true)).toFutureOption()
    collection.createIndex(Indexes.ascending(nameOf[Debit](_.label)), IndexOptions().unique(true)).toFutureOption()
    collection
      .createIndex(Indexes.ascending(nameOf[Debit](_.paymentHash)), IndexOptions().unique(true))
      .toFutureOption()
    collection
      .createIndex(Indexes.ascending(nameOf[Debit](_.playerAccountId), nameOf[Debit](_.status)))
      .toFutureOption()
  }

}
