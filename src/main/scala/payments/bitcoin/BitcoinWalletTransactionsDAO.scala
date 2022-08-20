package payments.bitcoin

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.bitcoin.{TxId, WalletTransaction}
import fr.acinq.bitcoin.Satoshi
import org.mongodb.scala.{MongoCollection, SingleObservable}
import payments.MongoDAO

import java.time.Instant
import scala.concurrent.ExecutionContext

object BitcoinInvoice {
  def apply(address: String, tx: WalletTransaction, metadata: String) = {
    new BitcoinInvoice(address = address,
                       metadata = metadata,
                       received = tx.timereceived,
                       satoshi = tx.amount.toSatoshi,
                       hex = Some(tx.txid),
                       confirmations = tx.confirmations)
  }
}

case class BitcoinInvoice(address: String,
                          metadata: String,
                          received: Instant,
                          confirmations: Int,
                          satoshi: Satoshi,
                          hex: Option[TxId])

object BitcoinInvoicesDAO {
  val collectionName = "bitcoin_invoices"
}
class BitcoinInvoicesDAO(val collection: MongoCollection[BitcoinInvoice])(implicit
                                                                          val executionContext: ExecutionContext)
    extends MongoDAO[BitcoinInvoice] {
  override def createIndexes(): List[SingleObservable[String]] = List(
//    createIndex(nameOf[BitcoinInvoice](_.hex), unique = true)
  )

  override def collectionName: String = BitcoinInvoicesDAO.collectionName
}
