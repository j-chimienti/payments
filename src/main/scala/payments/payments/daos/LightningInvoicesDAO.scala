package payments.payments.daos

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.bitcoin.MilliSatoshi
import com.mathbot.pay.lightning.Bolt11
import com.mathbot.pay.lightning.lightningcharge.LightningChargeInvoice
import org.mongodb.scala.model.{Filters, IndexOptions, Indexes}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.descending
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Completed, MongoCollection}
import payments.payments.models.{Credit, SecureIdentifier}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
case class LightningChargeInvoiceD(
    id: String,
    playerAccountId: SecureIdentifier,
    bolt11: Bolt11,
    description: String,
    expires_at: Instant,
    created_at: Instant,
    status: String,
    paid_at: Option[Instant],
    msatoshi_received: Option[MilliSatoshi]
)

object LightningChargeInvoiceD {
  def apply(invoice: LightningChargeInvoice, playerAccountId: SecureIdentifier): LightningChargeInvoiceD =
    LightningChargeInvoiceD(
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

class LightningInvoicesDAO(val collection: MongoCollection[LightningChargeInvoiceD])(implicit
                                                                                     executionContext: ExecutionContext)
    extends MongoCollectionTrait[LightningChargeInvoiceD] {

  def findByIdAndPlayer(id: String, playerAccountId: SecureIdentifier): Future[Option[LightningChargeInvoiceD]] =
    collection
      .find(
        Filters.and(equal(nameOf[LightningChargeInvoiceD](_.id), id),
                    equal(nameOf[LightningChargeInvoiceD](_.playerAccountId), playerAccountId))
      )
      .headOption()

  def updateStatus(id: String, status: String): Future[Option[UpdateResult]] =
    collection
      .updateOne(equal(nameOf[LightningChargeInvoiceD](_.id), id),
                 set(nameOf[LightningChargeInvoiceD](_.status), status))
      .toFutureOption()

  def bulkInsert(invoices: Seq[LightningChargeInvoice], playerAccountId: SecureIdentifier): Future[Option[Completed]] =
    collection.insertMany(invoices.map(LightningChargeInvoiceD(_, playerAccountId))).toFutureOption()

  def findLatest(latest: Int = 200): Future[Seq[LightningChargeInvoiceD]] =
    collection.find().sort(descending(nameOf[LightningChargeInvoiceD](_.created_at))).limit(latest).toFuture()

  def update(invoice: LightningChargeInvoice): Future[Option[UpdateResult]] =
    collection
      .updateOne(
        equal(nameOf[LightningChargeInvoiceD](_.id), invoice.id),
        combine(
          set(nameOf[LightningChargeInvoiceD](_.status), invoice.status.toString),
          set(nameOf[LightningChargeInvoiceD](_.paid_at), invoice.paid_at.orNull),
          set(nameOf[LightningChargeInvoiceD](_.msatoshi_received), invoice.msatoshi_received.orNull)
        )
      )
      .toFutureOption()

  def findByStatus(status: String): Future[Seq[LightningChargeInvoiceD]] =
    collection.find(equal(nameOf[LightningChargeInvoiceD](_.status), status)).toFuture()

  collection.createIndex(Indexes.ascending(nameOf[LightningChargeInvoiceD](_.id)), IndexOptions().unique(true))
  collection.createIndex(Indexes.ascending(nameOf[LightningChargeInvoiceD](_.bolt11)), IndexOptions().unique(true))
  collection.createIndex(
    Indexes.ascending(nameOf[LightningChargeInvoiceD](_.playerAccountId), nameOf[LightningChargeInvoiceD](_.status))
  )

}
