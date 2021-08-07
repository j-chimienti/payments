package payments.credits

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.Bolt11
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.{equal, gte}
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

  def find(playerAccountId: SecureIdentifier): Future[Seq[Credit]] =
    collection.find(byPlayerAccountId(playerAccountId)).toFuture()

  def find(bolt11: Bolt11): Future[Option[Credit]] =
    collection.find(equal(nameOf[Credit](_.bolt11), bolt11.toString)).headOption()

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
  collection.createIndex(Indexes.ascending(nameOf[Credit](_.bolt11)), IndexOptions().unique(true))
  collection.createIndex(Indexes.ascending(nameOf[Credit](_.playerAccountId)))
  collection.createIndex(Indexes.ascending(nameOf[Credit](_.created_at)))

}
