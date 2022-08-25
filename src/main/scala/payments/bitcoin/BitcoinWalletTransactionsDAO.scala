package payments.bitcoin

import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.bitcoin.{TxId, WalletTransaction}
import fr.acinq.bitcoin.Satoshi
import org.mongodb.scala.{MongoCollection, SingleObservable}
import payments.MongoDAO

import java.time.Instant
import scala.concurrent.ExecutionContext

object BitcoinWalletTransaction {
  def apply(address: String, tx: WalletTransaction, metadata: String): BitcoinWalletTransaction = {
    new BitcoinWalletTransaction(address = address,
                                 metadata = metadata,
                                 received = tx.timereceived,
                                 satoshi = tx.amount.toSatoshi,
                                 hex = Some(tx.txid),
                                 confirmations = tx.confirmations)
  }
}

case class BitcoinWalletTransaction(address: String,
                                    metadata: String,
                                    received: Instant,
                                    confirmations: Int,
                                    satoshi: Satoshi,
                                    hex: Option[TxId])

object BitcoinWalletTransactionsDAO {
  val collectionName = "bitcoin_invoices"
}
class BitcoinWalletTransactionsDAO(val collection: MongoCollection[BitcoinWalletTransaction])(
    implicit
    val executionContext: ExecutionContext
) extends MongoDAO[BitcoinWalletTransaction] {
  override def createIndexes(): List[SingleObservable[String]] = List(
//    createIndex(nameOf[BitcoinInvoice](_.hex), unique = true)
  )

  override def collectionName: String = BitcoinWalletTransactionsDAO.collectionName
}
