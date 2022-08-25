package payments.services

import com.mathbot.pay.bitcoin.AddressType.AddressType
import com.mathbot.pay.bitcoin.BitcoinJsonRpcClient
import payments.bitcoin.{BitcoinWalletTransaction, BitcoinWalletTransactionsDAO}

import scala.concurrent.ExecutionContext

class DatabaseBitcoinService(service: BitcoinJsonRpcClient, dao: BitcoinWalletTransactionsDAO)(
    implicit ec: ExecutionContext
) {

  def getnewaddress(
      label: Option[String] = None,
      addressType: Option[AddressType] = None,
      metadata: String
  ) = {

    for {
      addr <- service.getnewaddress(label, addressType)
//      r <- dao.insert(BitcoinWalletTransaction())

    } yield addr
  }
}
