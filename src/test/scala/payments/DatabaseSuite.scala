package payments

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.mongodb.scala.MongoClient
import org.scalatest._
import payments.wiring.PaymentsDatabaseWiring

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Try

abstract class DatabaseSuite
    extends AsyncWordSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with EitherValues
    with OptionValues
    with PaymentsDatabaseWiring
    with StrictLogging
    with org.scalatest.matchers.should.Matchers {
  lazy val config = ConfigFactory.load()

  lazy val mongoClient = MongoClient(config.getString("mongodb.url"))
  lazy val db = mongoClient.getDatabase(config.getString("mongodb.name"))
  def dropDb = {
    logger.info(s"Dropping db")
    val dropR = Await.result(db.drop().toFuture(), 10.seconds)
    logger.info(s"Dropped db $dropR")
  }
  override def beforeEach(): Unit = {
    dropDb
    // todo: creating indexes succeedes but always times out
    val ci = Try(Await.result(createIndexes(), 10.seconds))
    logger.info(s"Create indexes = {}", ci)
  }

}
