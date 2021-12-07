package payments.credits

import cats.data.NonEmptyList
import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.Bolt11
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.{equal, gte, in}
import org.mongodb.scala.model.{IndexOptions, Indexes}
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

  def findBulk(value: Seq[String]) = collection.find(in(nameOf[Credit](_.paymentHash),value)).toFuture()


  def find() = collection.find().toFuture()
  def find(playerAccountId: String): Future[Seq[Credit]] =
    collection.find(byPlayerAccountId(playerAccountId)).toFuture()

  def findByPaymentHash(paymentHash: String): Future[Option[Credit]] =
    collection.find(equal(nameOf[Credit](_.paymentHash),paymentHash)).headOption()

  def findByLabel(label: String): Future[Option[Credit]] =
    collection.find(equal(nameOf[Credit](_.label),label)).headOption()

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
