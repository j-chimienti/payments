package payments

import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.dwickern.macros.NameOf.nameOf
import com.mongodb.MongoWriteException
import com.mongodb.client.model.{Indexes, ReturnDocument}
import com.typesafe.scalalogging.StrictLogging
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.jsonSchema
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexOptions}
import org.mongodb.scala.{Completed, MongoCollection, MongoDatabase, SingleObservable}
import payments.debits.LightningPayment
import play.api.libs.json.{JsValue, Json}

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

  def createIndex(fieldName: String, unique: Boolean) = {
    collection.createIndex(Indexes.ascending(fieldName), IndexOptions().unique(unique))
  }

  def expireAfter(t: FiniteDuration): IndexOptions =
    IndexOptions()
      .expireAfter(t.toSeconds, SECONDS)

  def createUniqueBolt11Index() = {
    val n = nameOf[LightningPayment](_.bolt11)
    collection
      .createIndex(Indexes.ascending(n), Unique)

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

  /**
   *
   * @return the documents that pass validation
   */
  def findBySchema: Future[Either[String, Seq[BsonDocument]]] =
    schemaStr
      .map(
        value =>
          collection
            .withDocumentClass[BsonDocument]
            .find(jsonSchema(BsonDocument(value.toString)))
            .toFuture()
            .map(Right(_))
      )
      .getOrElse(FastFuture.successful(Left(s"No schema define for collection=${collection}")))

  def compareToSchema: Future[Either[(String, String, Long), Long]] =
    for {
      numOfDocs <- count
      validSchemaItems <- findBySchema
    } yield {

      validSchemaItems
        .map(f => {
          if (numOfDocs != f.length)
            Left("Validation failed collection={} items={}", collectionName, math.abs(numOfDocs - f.size))
          else Right(numOfDocs)
        })
        .getOrElse(Left("Validation failed collection={} items={}", collectionName, numOfDocs))
    }

  def isDuplicateException(err: Throwable): Boolean =
    err match {
      case e: MongoWriteException => e.getCode == 11000
      case _ => false
    }

}

object MongoDAO extends StrictLogging {

  object ValidationLevels extends Enumeration {
    type ValidationLevels = Value
    val strict, moderate = Value
  }

  def refreshValidations(collections: Set[(JsValue, String)], db: MongoDatabase)(implicit m: Materializer,
                                                                                 ec: ExecutionContext) = {
    Source(collections)
      .mapAsyncUnordered(parallelism = 1) {
        case (schema, dao) =>
          (for {
            a <- removeValidation(db, dao)
            c <- createSchema(
              jsonSchema = schema,
              collectionName = dao,
              level = ValidationLevels.strict,
            )(db)
          } yield (dao, a, c)) recover (err => {
            logger.warn(s"Error creating validation collection={} error={}", dao, err)
            (dao, None, None)
          })
      }
      .runWith(Sink.seq)
  }

  def dropIndexes(c: Set[MongoCollection[_]])(implicit m: Materializer) =
    Source(c)
      .mapAsyncUnordered(3)(
        c =>
          c.dropIndexes()
            .map(r => Some(r))
            .recover(err => {
              logger.warn("Error dropping index error={}", err)
              None
            })
            .toFuture()
      )
      .runWith(Sink.seq)

  def removeValidation[T](db: MongoDatabase, collectionName: String) = {
    db.runCommand(
        BsonDocument(Json.obj("collMod" -> collectionName, "validationLevel" -> "off").toString)
      )
      .toFutureOption()
  }

  def createSchema(
      jsonSchema: JsValue,
      collectionName: String,
      level: ValidationLevels.ValidationLevels,
      validationAction: String = "error"
  )(db: MongoDatabase) = {
    db.runCommand(
        BsonDocument(
          Json
            .obj(
              "collMod" -> collectionName,
              "validator" -> Json.obj(
                "$jsonSchema" -> jsonSchema
              ),
              "validationLevel" -> level.toString,
              "validationAction" -> validationAction
            )
            .toString
        )
      )
      .toFutureOption()
  }
}
