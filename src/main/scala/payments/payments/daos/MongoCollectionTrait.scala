package payments.payments.daos

import com.github.dwickern.macros.NameOf.nameOf
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.result.DeleteResult
import org.mongodb.scala.{Completed, MongoCollection, MongoWriteException}
import payments.payments.models.{Debit, SecureIdentifier}

import scala.concurrent.Future

trait MongoCollectionTrait[T] {

  def collection: MongoCollection[T]

  def insert(t: T): Future[Option[Completed]] = insertOne(t)
  def insertOne(t: T): Future[Option[Completed]] = collection.insertOne(t).toFutureOption

  def count: Future[Long] = collection.countDocuments().toFuture

  val groupStatusStatement = BsonDocument("""{$group: { _id: "$status", count: {$sum: 1} }}""")

  val matchAll = BsonDocument("{}")

  def deleteAll(): Future[Option[DeleteResult]] = collection.deleteMany(matchAll).toFutureOption()

  def isDuplicateException(err: Throwable): Boolean =
    err match {
      case e: MongoWriteException => e.getCode == 11000
      case _ => false
    }

  def byPlayerAccountId(playerAccountId: SecureIdentifier) =
    equal(nameOf[Debit](_.playerAccountId), playerAccountId.toString)
}
