package payments.lightninginvoices

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.bitcoin.MilliSatoshi
import com.mathbot.pay.lightning.LightningInvoiceStatus.LightningInvoiceStatus
import com.mathbot.pay.lightning.{Bolt11, LightningInvoiceStatus, ListInvoice}
import com.mathbot.pay.lightning.lightningcharge.LightningChargeInvoice
import com.mathbot.pay.lightning.url.InvoiceWithDescriptionHash
import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.descending
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import payments.models.SecureIdentifier
import payments.utils.MongoCollectionTrait

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class LightningInvoiceModel(
    id: String,
    playerAccountId: String,
    bolt11: Bolt11,
    description: String,
    expires_at: Instant,
    created_at: Instant,
    status: String,
    paid_at: Option[Instant],
    msatoshi_received: Option[MilliSatoshi]
) {
  lazy val invoiceStatus = LightningInvoiceStatus.withName(status)
}

object LightningInvoiceModel {
  def fromD(invoice: ListInvoice, playerAccountId: String): LightningInvoiceModel =
    LightningInvoiceModel(
      id = invoice.label,
      playerAccountId = playerAccountId,
      bolt11 = invoice.bolt11.get,
      description = invoice.description,
      expires_at = Instant.ofEpochSecond(invoice.expires_at),
      created_at = Instant.now(),
      status = LightningInvoiceStatus.unpaid.toString,
      paid_at = invoice.paid_at,
      msatoshi_received = invoice.msatoshi_received
    )

  def apply(invoice: LightningChargeInvoice, playerAccountId: String): LightningInvoiceModel =
    LightningInvoiceModel(
      id = invoice.id,
      playerAccountId = playerAccountId,
      bolt11 = invoice.bolt11,
      description = invoice.description,
      expires_at = invoice.expires_at,
      created_at = invoice.created_at,
      status = invoice.status.toString,
      paid_at = invoice.paid_at,
      msatoshi_received = invoice.msatoshi_received
    )
}

object LightningInvoicesDAO {
  val collectionName = "invoices_lightning"

}

class LightningInvoicesDAO(val collection: MongoCollection[LightningInvoiceModel])(implicit
                                                                                   executionContext: ExecutionContext)
    extends MongoCollectionTrait[LightningInvoiceModel] {

  def findByIdAndPlayer(id: String, playerAccountId: String): Future[Option[LightningInvoiceModel]] =
    collection
      .find(
        Filters.and(equal(nameOf[LightningInvoiceModel](_.id), id),
                    equal(nameOf[LightningInvoiceModel](_.playerAccountId), playerAccountId))
      )
      .headOption()

  def updateStatus(id: String, status: String): Future[Option[UpdateResult]] =
    collection
      .updateOne(equal(nameOf[LightningInvoiceModel](_.id), id), set(nameOf[LightningInvoiceModel](_.status), status))
      .toFutureOption()

  def findLatest(perPage: Int = 200, page: Int = 0): Future[Seq[LightningInvoiceModel]] =
    collection
      .find()
      .sort(descending(nameOf[LightningInvoiceModel](_.created_at)))
      .skip(page)
      .limit(perPage)
      .toFuture()

  def update(invoice: LightningChargeInvoice): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningInvoiceModel](_.id), invoice.id),
        combine(
          set(nameOf[LightningInvoiceModel](_.status), invoice.status.toString),
          set(nameOf[LightningInvoiceModel](_.paid_at), invoice.paid_at.orNull),
          set(nameOf[LightningInvoiceModel](_.msatoshi_received), invoice.msatoshi_received.orNull)
        )
      )
      .toFutureOption()

  def findByStatus(status: LightningInvoiceStatus): Future[Seq[LightningInvoiceModel]] =
    collection.find(equal(nameOf[LightningInvoiceModel](_.status), status.toString)).toFuture()

  collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.id)), IndexOptions().unique(true))
  collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.bolt11)), IndexOptions().unique(true))
  collection.createIndex(
    Indexes.ascending(nameOf[LightningInvoiceModel](_.playerAccountId), nameOf[LightningInvoiceModel](_.status))
  )
  collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.created_at)))

}
