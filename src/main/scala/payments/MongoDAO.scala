package payments

import akka.http.scaladsl.util.FastFuture
import com.github.dwickern.macros.NameOf.nameOf
import com.mongodb.client.model.{Indexes, ReturnDocument}
import com.typesafe.scalalogging.StrictLogging
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexOptions}
import org.mongodb.scala.{Completed, MongoCollection, SingleObservable}
import payments.debits.LightningPayment
import play.api.libs.json.JsValue

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

trait MongoDAO[T] extends StrictLogging {
  def collection: MongoCollection[T]

  // todo: need to recover on each obs
  def createIndexes(): List[SingleObservable[String]]

  implicit def executionContext: ExecutionContext

  def collectionName: String

  final val Unique = IndexOptions().unique(true)
  final val ReturnAfter = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)

  def expireAfter(t: FiniteDuration): IndexOptions =
    IndexOptions()
      .expireAfter(t.toSeconds, SECONDS)



  def createUniqueBolt11Index() = {
    val n = nameOf[LightningPayment](_.bolt11)
    collection
      .createIndex(Indexes.ascending(n), Unique)

  }

  def createTokenIdAndStatusIndex() = {
    val t = nameOf[LightningPayment](_.tokenId)
    val s = nameOf[LightningPayment](_.status)
    collection.createIndex(Indexes.ascending(t, s))
  }

  def createUniqueLabelIndex() = {
    val n = nameOf[LightningPayment](_.label)
    collection
      .createIndex(Indexes.ascending(n), Unique)

  }

  def createUniquePaymentHashIndex() = {
    val n = nameOf[LightningPayment](_.payment_hash)
    collection
      .createIndex(Indexes.ascending(n), Unique)

  }

  def insert(t: T): Future[Option[Completed]] = collection.insertOne(t).toFutureOption()

  def insertMany(t: Seq[T]): Future[Option[Completed]] =
    if (t.isEmpty) FastFuture.successful(None)
    else collection.insertMany(t).toFutureOption()

  def count: Future[Long] = collection.countDocuments().toFuture()

  def schemaStr: Option[JsValue] = None

  //  /**
  //   *
  //   * @return the documents that pass validation
  //   */
  //  def findBySchema: Future[Seq[T]] =
  //    schemaStr match {
  //      case Some(value) =>
  //        collection.find[](jsonSchema(BsonDocument(value.toString))).toFuture()
  //      case None =>
  //        logger.info("No schema define for collection={}", collectionName)
  //        FastFuture.successful(Seq())
  //    }
  //
  //  def compareToSchema = {
  //    for {
  //      c <- count
  //      f <- findBySchema
  //    } yield {
  //      if (c != f.length) logger.warn("Validation failed collection={} items={}", collectionName, math.abs(c - f.size))
  //      (c, f)
  //    }
  //
  //  }
}
