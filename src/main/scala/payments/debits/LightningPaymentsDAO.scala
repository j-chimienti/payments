package payments.debits
import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.PayStatus.PayStatus
import com.mathbot.pay.lightning.{Bolt11, ListPay, PayStatus}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates
import org.mongodb.scala.model.Updates.currentDate
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{MongoCollection, _}
import payments.MongoDAO
import payments.debits.LightningPayment
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class LightningPaymentsDAO(val collection: MongoCollection[LightningPayment])(
  implicit val executionContext: ExecutionContext
) extends MongoDAO[LightningPayment] {
  val collectionName = LightningPaymentsDAO.collectionName
  def findByBolt11(bolt11: Bolt11): Future[Option[LightningPayment]] =
    collection.find(equal(nameOf[LightningPayment](_.bolt11), bolt11.toString)).headOption

  def updateOne(bolt11: Bolt11,
                status: PayStatus): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningPayment](_.bolt11), bolt11.bolt11),
        Updates.combine(
          currentDate(nameOf[LightningPayment](_.updatedAt)),
          Updates.set(nameOf[LightningPayment](_.status), status.toString),
        )
      )
      .toFutureOption()

  def updateOne(bolt11: Bolt11, listPay: ListPay): Future[Option[UpdateResult]] =
    updateOne(bolt11 = bolt11, status = listPay.status)

  def findPending: Future[Seq[LightningPayment]] =
    collection.find(equal(nameOf[LightningPayment](_.status), PayStatus.pending.toString)).toFuture()
  override def createIndexes() = {
    createUniqueBolt11Index() ::
      createUniqueLabelIndex() ::
      createTokenIdAndStatusIndex() :: Nil
  }

  override def insert(t: LightningPayment): Future[Option[Completed]] =
    super.insert(t.copy(payment_hash = Some(t.paymentHash)))

  override val schemaStr = None // todo
}

object LightningPaymentsDAO {

//  lazy val schemaStr =
//    Json.parse(scala.io.Source.fromResource("schemas/lightning_payments.bsonSchema.json").getLines().mkString(""))
  final val collectionName = "lightning_payments"
}
