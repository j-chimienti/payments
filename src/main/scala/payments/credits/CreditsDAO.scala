package payments.credits

import cats.data.NonEmptyList
import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.{Bolt11, ListInvoice}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.{equal, gte, in, or}
import org.mongodb.scala.model.{IndexOptions, Indexes, Updates}
import payments.models.SecureIdentifier
import payments.utils.MongoCollectionTrait

import java.time.Instant
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}

object CreditsDAO {
  val collectionName = "credit"

}
class CreditsDAO(
                  val collection: MongoCollection[Credit]
                )(implicit ec: ExecutionContext)
  extends MongoCollectionTrait[Credit] {
  def insertMany(c: NonEmptyList[Credit]) = collection.insertMany(c.toList).toFuture()

  def findBulk(value: Seq[String]) = collection.find(in(nameOf[Credit](_.paymentHash), value)).toFuture()

  def find() = collection.find().toFuture()
  def find(playerAccountId: String): Future[Seq[Credit]] =
    collection.find(byPlayerAccountId(playerAccountId)).toFuture()

  def update(invoice: ListInvoice) = {
    collection
      .updateOne(
        or(
          equal(nameOf[Credit](_.label), invoice.label),
          equal(nameOf[Credit](_.paymentHash), invoice.payment_hash),
        ),
        Updates.combine(
          Updates.set(nameOf[Credit](_.paymentHash), invoice.payment_hash),
          Updates.set(nameOf[Credit](_.description), invoice.description),
          Updates.set(nameOf[Credit](_.paid_at), invoice.paid_at.orNull),
          Updates.set(nameOf[Credit](_.pay_index), invoice.pay_index.getOrElse(null)),
          Updates.set(nameOf[Credit](_.local_offer_id), invoice.local_offer_id.orNull),
          Updates.set(nameOf[Credit](_.msatoshi_received), invoice.msatoshi_received.orNull),
          Updates.set(nameOf[Credit](_.bolt11), invoice.bolt11.orNull),
          Updates.set(nameOf[Credit](_.bolt12), invoice.bolt12.orNull)
        )
      )
      .toFutureOption()
  }

  def findByPaymentHash(paymentHash: String): Future[Option[Credit]] =
    collection.find(equal(nameOf[Credit](_.paymentHash), paymentHash)).headOption()

  def findByLabel(label: String): Future[Option[Credit]] =
    collection.find(equal(nameOf[Credit](_.label), label)).headOption()

  def findWithin(timeSpan: FiniteDuration = 1.hour): Future[Seq[Credit]] =
    collection
      .find(
        gte(
          nameOf[Credit](_.created_at),
          Instant
            .now()
            .minusSeconds(timeSpan.toSeconds)
        )
      )
      .toFuture()

  collection.createIndex(Indexes.ascending(nameOf[Credit](_.label)), IndexOptions().unique(true))
  collection.createIndex(Indexes.ascending(nameOf[Credit](_.paymentHash)), IndexOptions().unique(true))
  collection.createIndex(Indexes.ascending(nameOf[Credit](_.playerAccountId)))
  collection.createIndex(Indexes.ascending(nameOf[Credit](_.created_at)))

}
