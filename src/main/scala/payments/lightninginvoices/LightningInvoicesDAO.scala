package payments.lightninginvoices

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.LightningInvoiceStatus.LightningInvoiceStatus
import com.mathbot.pay.lightning.{Bolt11, LightningInvoiceStatus}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.{ascending, descending}
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{IndexOptions, _}
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{MongoCollection, SingleObservable}
import payments.MongoDAO
import payments.models.LightningInvoiceModel
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.language.postfixOps

object LightningInvoicesDAO {
  val collectionName = "invoices_lightning"
  lazy val schema: JsValue =
    Json.parse(Source.fromResource("schemas/lightning_invoices.bsonSchema.json").getLines().mkString(""))

}

class LightningInvoicesDAO(val collection: MongoCollection[LightningInvoiceModel])(implicit
                                                                                   val
                                                                                   executionContext: ExecutionContext)
    extends MongoDAO[LightningInvoiceModel] {
  def findByBolt11(bolt11: Bolt11): Future[Option[LightningInvoiceModel]] =
    collection
      .find(
        equal(nameOf[LightningInvoiceModel](_.bolt11), bolt11.bolt11),
      )
      .headOption()

  def findByLabel(label: String): Future[Option[LightningInvoiceModel]] =
    collection
      .find(
        equal(nameOf[LightningInvoiceModel](_.label), label),
      )
      .headOption()

  def findByMetadata(metadata: String): Future[Seq[LightningInvoiceModel]] =
    collection
      .find(
        equal(nameOf[LightningInvoiceModel](_.metadata), metadata),
      )
      .toFuture()

  val collectionName: String = LightningInvoicesDAO.collectionName
  override val schemaStr: Option[JsValue] = Some(LightningInvoicesDAO.schema)

  def findByLabelAndMetadata(label: String, metadata: String): Future[Option[LightningInvoiceModel]] =
    collection
      .find(
        Filters.and(
          equal(nameOf[LightningInvoiceModel](_.label), label),
          equal(nameOf[LightningInvoiceModel](_.metadata), metadata)
        )
      )
      .headOption()

  def updateStatus(label: String, status: LightningInvoiceStatus): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningInvoiceModel](_.label), label),
        set(nameOf[LightningInvoiceModel](_.status), status.toString)
      )
      .toFutureOption()

  def findLatest(perPage: Int = 200, page: Int = 0): Future[Seq[LightningInvoiceModel]] =
    collection
      .find()
      .sort(descending(nameOf[LightningInvoiceModel](_.created_at)))
      .skip(page)
      .limit(perPage)
      .toFuture()

  def update(invoice: com.mathbot.pay.lightning.ListInvoice): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningInvoiceModel](_.label), invoice.label),
        combine(
          set(nameOf[LightningInvoiceModel](_.status), invoice.status.toString),
          set(nameOf[LightningInvoiceModel](_.paid_at), invoice.paid_at.orNull),
          set(nameOf[LightningInvoiceModel](_.payment_preimage), invoice.payment_preimage.orNull),
          set(nameOf[LightningInvoiceModel](_.pay_index), invoice.pay_index.getOrElse(null)),
          set(nameOf[LightningInvoiceModel](_.amount_received_msat), invoice.amount_received_msat.getOrElse(null)),
          set(nameOf[LightningInvoiceModel](_.amount_msat), invoice.amount_msat.getOrElse(null))
        )
      )
      .toFutureOption()

  def findByStatus(status: LightningInvoiceStatus): Future[Seq[LightningInvoiceModel]] =
    collection.find(equal(nameOf[LightningInvoiceModel](_.status), status.toString)).toFuture()

  def findByPaymentHash(ph: String): Future[Option[LightningInvoiceModel]] =
    collection.find(equal(nameOf[LightningInvoiceModel](_.payment_hash), ph)).headOption()

  def expiredInvoicesTTL =
    collection.createIndex(
      ascending(nameOf[LightningInvoiceModel](_.created_at)),
      expireAfter(7.days).partialFilterExpression(
        equal(nameOf[LightningInvoiceModel](_.status), LightningInvoiceStatus.expired.toString)
      )
    )

  override def createIndexes(): List[SingleObservable[String]] =
    List(
      createIndex(nameOf[LightningInvoiceModel](_.label), true),
      createIndex(nameOf[LightningInvoiceModel](_.bolt11), true),
      collection.createIndex(
        Indexes.ascending(nameOf[LightningInvoiceModel](_.metadata), nameOf[LightningInvoiceModel](_.status))
      ),
      createIndex(nameOf[LightningInvoiceModel](_.created_at), false),
      collection.createIndex(
        ascending(nameOf[LightningInvoiceModel](_.payment_hash)),
        IndexOptions().unique(true)
      ),
      expiredInvoicesTTL,
    )

}
