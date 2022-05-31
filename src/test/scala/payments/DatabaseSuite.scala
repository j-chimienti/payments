package payments

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
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
    with org.scalatest.matchers.should.Matchers {
  lazy val config = ConfigFactory.load()

  implicit val as = ActorSystem("test")

  implicit val m = Materializer.matFromSystem(as)
  lazy val mongoClient = MongoClient(config.getString("mongodb.url"))
  lazy val db = mongoClient.getDatabase(config.getString("mongodb.name"))
  override def beforeEach(): Unit = {
    val dropR = Await.result(db.drop().toFuture(), 2.seconds)
    // todo: creating indexes succeedes but always times out
    val ci = Try(Await.result(createIndexes(), 2.seconds))
    val ci1 = Try(Await.result(refreshAllValidations(m), 2.seconds))
  }

  override def afterAll(): Unit = {
    as.terminate()
    mongoClient.close()
  }

}
