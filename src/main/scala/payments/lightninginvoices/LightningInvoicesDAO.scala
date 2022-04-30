package payments.lightninginvoices

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.LightningInvoiceStatus.LightningInvoiceStatus
import com.mathbot.pay.lightning._
import org.mongodb.scala.model.Filters.{equal, or}
import org.mongodb.scala.model.Indexes.descending
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{MongoCollection, SingleObservable}
import payments.MongoDAO
import payments.models.LightningInvoiceModel
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.language.postfixOps

object LightningInvoicesDAO {
  val collectionName = "invoices_lightning"

  val schema = Json.parse(Source.fromResource("schemas/lightning_invoices.bsonSchema.json").getLines().mkString(""))

}

class LightningInvoicesDAO(val collection: MongoCollection[LightningInvoiceModel])(implicit
                                                                                   val
                                                                                   executionContext: ExecutionContext)
    extends MongoDAO[LightningInvoiceModel] {
  def findByBolt11(bolt11: Bolt11) =
    collection
      .find(
        equal(nameOf[LightningInvoiceModel](_.bolt11), bolt11.bolt11),
      )
      .headOption()

  val collectionName: String = LightningInvoicesDAO.collectionName
  override val schemaStr = None // todo Some(LightningInvoicesDAO.schema)

  def findByIdAndPlayer(label: String, playerAccountId: String): Future[Option[LightningInvoiceModel]] =
    collection
      .find(
        Filters.and(
          equal(nameOf[LightningInvoiceModel](_.label), label),
          equal(nameOf[LightningInvoiceModel](_.metadata), playerAccountId)
        )
      )
      .headOption()

  def updateStatus(label: String, status: String): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningInvoiceModel](_.label), label),
        set(nameOf[LightningInvoiceModel](_.status), status)
      )
      .toFutureOption()

  def findLatest(perPage: Int = 200, page: Int = 0): Future[Seq[LightningInvoiceModel]] =
    collection
      .find()
      .sort(descending(nameOf[LightningInvoiceModel](_.created_at)))
      .skip(page)
      .limit(perPage)
      .toFuture()

  def update(invoice: ListInvoice): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningInvoiceModel](_.payment_hash), invoice.payment_hash),
        combine(
          set(nameOf[LightningInvoiceModel](_.status), invoice.status.toString),
          set(nameOf[LightningInvoiceModel](_.paid_at), invoice.paid_at.orNull),
          set(nameOf[LightningInvoiceModel](_.pay_index), invoice.pay_index.getOrElse(null)),
          set(nameOf[LightningInvoiceModel](_.msatoshi_received), invoice.amount_received_msat.getOrElse(null))
        )
      )
      .toFutureOption()

  def findByStatus(status: LightningInvoiceStatus): Future[Seq[LightningInvoiceModel]] =
    collection.find(equal(nameOf[LightningInvoiceModel](_.status), status.toString)).toFuture()

  override def createIndexes(): List[SingleObservable[String]] =
    List(
      createIndex(nameOf[LightningInvoiceModel](_.label), true),
      createIndex(nameOf[LightningInvoiceModel](_.bolt11), true),
      collection.createIndex(
        Indexes.ascending(nameOf[LightningInvoiceModel](_.metadata), nameOf[LightningInvoiceModel](_.status))
      ),
      collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.created_at)))
    )

}
