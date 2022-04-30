package payments.wiring

import com.softwaremill.macwire.{wire, wireSet}
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistries}
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import payments.MongoDAO
import payments.codecs._
import payments.debits.{LightningPayment, LightningPaymentsDAO}
import payments.lightninginvoices.LightningInvoicesDAO
import payments.models.LightningInvoiceModel

import scala.concurrent.{ExecutionContext, Future}

trait PaymentsDatabaseWiring {

  def db: MongoDatabase
  implicit def executionContext: ExecutionContext

  val sic = wire[SecureIdentifierCodec]
  val bic = wire[Bolt11Codec]
  val satc = wire[SatoshiCodec]
  val mic = wire[MilliSatoshiCodec]

  val codecs: Set[Codec[_]] = wireSet[Codec[_]]

  lazy val mongoCodecProviders: Seq[CodecProvider] = Seq(
    Macros.createCodecProvider[LightningPayment](),
    Macros.createCodecProvider[LightningInvoiceModel]()
  )

  private val codecRegistry = fromRegistries(
    fromProviders(
      mongoCodecProviders: _*
    ),
    CodecRegistries.fromCodecs(codecs.toSeq: _*),
    org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  )

  val lightningPaymentColl: MongoCollection[LightningPayment] = db
    .getCollection[LightningPayment](LightningPaymentsDAO.collectionName)
    .withCodecRegistry(codecRegistry)

  val lnInvColl: MongoCollection[LightningInvoiceModel] =
    db.getCollection[LightningInvoiceModel](LightningInvoicesDAO.collectionName)
      .withCodecRegistry(codecRegistry)

  lazy val debitsDao: LightningPaymentsDAO = wire[LightningPaymentsDAO]
  lazy val lightningInvoicesDAO = wire[LightningInvoicesDAO]

  lazy val daos = wireSet[MongoDAO[_]]

  def createIndexes() = Future.sequence(daos.flatMap(d => d.createIndexes().map(_.toFutureOption())))
}
