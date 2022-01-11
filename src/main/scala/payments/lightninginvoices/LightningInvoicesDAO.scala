package payments.lightninginvoices

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.bitcoin.MilliSatoshi
import com.mathbot.pay.lightning.LightningInvoiceStatus.LightningInvoiceStatus
import com.mathbot.pay.lightning.lightningcharge.LightningChargeInvoice
import com.mathbot.pay.lightning.url.CreateInvoiceWithDescriptionHash
import com.mathbot.pay.lightning._
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.{equal, or}
import org.mongodb.scala.model.Indexes.descending
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import payments.models.LightningInvoiceModel
import payments.utils.MongoCollectionTrait

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps



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

  def update(invoice: ListInvoice): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        or(
          equal(nameOf[LightningInvoiceModel](_.paymentHash), invoice.payment_hash),
          equal(nameOf[LightningInvoiceModel](_.bolt11), invoice.bolt11.orNull),
          equal(nameOf[LightningInvoiceModel](_.id), invoice.label),
        ),
        combine(
          set(nameOf[LightningInvoiceModel](_.status), invoice.status.toString),
          set(nameOf[LightningInvoiceModel](_.paid_at), invoice.paid_at.orNull),
          set(nameOf[LightningInvoiceModel](_.pay_index), invoice.pay_index.getOrElse(null)),
          set(nameOf[LightningInvoiceModel](_.paymentHash), invoice.payment_hash),
          set(nameOf[LightningInvoiceModel](_.description), invoice.description),
          set(nameOf[LightningInvoiceModel](_.msatoshi_received), invoice.msatoshi_received.orNull)
        )
      )
      .toFutureOption()

  def update(invoice: LightningChargeInvoice): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningInvoiceModel](_.id), invoice.id),
        combine(
          set(nameOf[LightningInvoiceModel](_.status), invoice.status.toString),
          set(nameOf[LightningInvoiceModel](_.paid_at), invoice.paid_at.orNull),
          set(nameOf[LightningInvoiceModel](_.pay_index), invoice.pay_index.getOrElse(null)),
          set(nameOf[LightningInvoiceModel](_.description), invoice.description),
          set(nameOf[LightningInvoiceModel](_.paymentHash), invoice.rhash),
          set(nameOf[LightningInvoiceModel](_.msatoshi_received), invoice.msatoshi_received.orNull)
        )
      )
      .toFutureOption()

  def findByStatus(status: LightningInvoiceStatus): Future[Seq[LightningInvoiceModel]] =
    collection.find(equal(nameOf[LightningInvoiceModel](_.status), status.toString)).toFuture()

  collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.id)), IndexOptions().unique(true))
  collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.paymentHash)), IndexOptions().unique(true))
  collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.bolt11)), IndexOptions().unique(true))
  collection.createIndex(
    Indexes.ascending(nameOf[LightningInvoiceModel](_.playerAccountId), nameOf[LightningInvoiceModel](_.status))
  )
  collection.createIndex(Indexes.ascending(nameOf[LightningInvoiceModel](_.created_at)))

}
