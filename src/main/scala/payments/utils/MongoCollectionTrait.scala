package payments.utils

import com.github.dwickern.macros.NameOf.nameOf
import com.typesafe.scalalogging.StrictLogging
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{Completed, MongoCollection, MongoWriteException}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{FindOneAndUpdateOptions, ReturnDocument}
import org.mongodb.scala.result.DeleteResult
import payments.debits.Debit
import payments.models.SecureIdentifier

import scala.concurrent.Future
import scala.language.postfixOps

trait MongoCollectionTrait[T] extends StrictLogging {

  def collection: MongoCollection[T]

  def insert(t: T): Future[Option[Completed]] = insertOne(t)
  def insertOne(t: T): Future[Option[Completed]] = collection.insertOne(t).toFutureOption

  def count: Future[Long] = collection.countDocuments().toFuture

  val groupStatusStatement = BsonDocument("""{$group: { _id: "$status", count: {$sum: 1} }}""")

  val matchAll = BsonDocument("{}")

  def deleteAll(): Future[Option[DeleteResult]] = collection.deleteMany(matchAll).toFutureOption()

  val ReturnAfter = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
  def isDuplicateException(err: Throwable): Boolean =
    err match {
      case e: MongoWriteException => e.getCode == 11000
      case _ => false
    }

  def byPlayerAccountId(playerAccountId: String) =
    equal(nameOf[Debit](_.playerAccountId), playerAccountId)
}
