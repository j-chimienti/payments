package payments.wiring

import com.softwaremill.macwire.{wire, wireSet}
import org.bson.codecs.Codec
import org.bson.codecs.configuration.{CodecProvider, CodecRegistries}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.mongodb.scala.bson.codecs.Macros
import payments.codecs._
import payments.credits.{Credit, CreditsDAO}
import payments.debits.{Debit, DebitsDAO}
import payments.lightninginvoices.{LightningChargeInvoiceD, LightningInvoicesDAO}

import scala.concurrent.ExecutionContext

trait PaymentsDatabaseWiring {

  def db: MongoDatabase
  implicit def executionContext: ExecutionContext

  val sic = wire[SecureIdentifierCodec]
  val bic = wire[Bolt11Codec]
  val satc = wire[SatoshiCodec]
  val mic = wire[MilliSatoshiCodec]

  val codecs: Set[Codec[_]] = wireSet[Codec[_]]

  lazy val mongoCodecProviders: Seq[CodecProvider] = Seq(
    Macros.createCodecProvider[Credit](),
    Macros.createCodecProvider[Debit](),
    Macros.createCodecProvider[LightningChargeInvoiceD]()
  )

  private val codecRegistry = fromRegistries(
    fromProviders(
      mongoCodecProviders: _*
    ),
    CodecRegistries.fromCodecs(codecs.toSeq: _*),
    org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  )


  val creditColl: MongoCollection[Credit] = db
    .getCollection[Credit](CreditsDAO.collectionName)
    .withCodecRegistry(codecRegistry)

  val debitColl: MongoCollection[Debit] = db
    .getCollection[Debit](DebitsDAO.collectionName)
    .withCodecRegistry(codecRegistry)

  val lnInvColl: MongoCollection[LightningChargeInvoiceD] =
    db.getCollection[LightningChargeInvoiceD](LightningInvoicesDAO.collectionName)
      .withCodecRegistry(codecRegistry)

  lazy val creditsDao: CreditsDAO = wire[CreditsDAO]
  lazy val debitsDao: DebitsDAO = wire[DebitsDAO]
  lazy val invoicesDao = wire[LightningInvoicesDAO]
}
