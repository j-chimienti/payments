package payments

import cats.implicits.catsSyntaxOptionId
import com.mathbot.pay.lightning.PayService.PlayerPayment__IN
import com.mathbot.pay.lightning.{Bolt11, PayStatus}
import org.mongodb.scala.MongoWriteException
import payments.debits.LightningPayment
import com.mathbot.pay.SecureIdentifier

class LightningPaymentsDAOTest extends DatabaseSuite {

  val bitrefillBolt11 =
    "lnbc22435140n1p0zxgcupp5hkn3lnlkk6kmq0670a9u2xd8reuefc7dw704pwe7dqqm06nnzpcsdz2gf5hgun9ve5kcmpqv9jx2v3e8pjkgtf5xaskxtf5venxvttp8pjrvttrvgerzvm9x93r2dehxgfppjue4tflpg2hule862xylcsnu0p0mrjvmnxqrp9s2xnqfz900f0mq36dnkseqped68nzjm2nvh85uxw4nkhezkd4zzlymw93q96mf9glupqxrfd46rps7dztqm5rerusxpx77curwrpal9qqenm4p0"
  val b2 = Bolt11(
    "lnbc9u1p02hg8xpp5vszfk9k7n7cur8s72d8acnnfgah3sstfzsg88u30asuw2me8xevqdq9venkycqzpgs9hmfca78h8hz4f270l5ulevd3r95je2gxjqv9zplxqk6fm6hs4hd9fhg5j7u9r8qc3mx08hs4thme097xjfp50yv8ddt0p5v09vp4sqsu6f47"
  )
  val bolt11 = Bolt11(bitrefillBolt11)
  val pp = PlayerPayment__IN(source = "???", playerId = "???", bolt11 = bolt11, callbackURL = None)
  val debit = LightningPayment.pending(pp)
  "LightningDebitsDAO" should {

    "start empty" in {
      lightningPaymentsDAO.count.map(r => r shouldBe 0)
    }
    "add debit" in {

      for {
        _ <- lightningPaymentsDAO.insert(debit)
        Some(fc) <- lightningPaymentsDAO.findByBolt11(debit.bolt11)
        c <- lightningPaymentsDAO.count
      } yield {
        fc.metadata shouldBe debit.metadata
        fc.label shouldBe debit.label
        fc.status shouldBe debit.status
        fc.payStatus shouldBe PayStatus.pending
        fc.amount_msat shouldBe debit.amount_msat
        fc.bolt11 shouldBe debit.bolt11
        fc.amount_sent_msat shouldBe debit.amount_sent_msat
        fc.payment_hash shouldBe debit.payment_hash
        c shouldBe 1
      }
    }
    "fail to add duplicate debit" in {
      for {
        _ <- lightningPaymentsDAO.insert(debit)
        r <- recoverToExceptionIf[MongoWriteException](
          lightningPaymentsDAO.insert(debit)
        ).map(err => assert(err.getError.getCode === 11000))
      } yield r
    }
    val pendingDebit = LightningPayment.pending {
      PlayerPayment__IN(
        source = "???",
        playerId = "???",
        bolt11 = Bolt11(
          "lnbc500n1pscyu7rpp5urqzug5lf56s8sh37g387vzgd8egp59c7twv8vtrfhvgxs9l4vmsdp0gejk2epqgd5xjcmtv4h8xgzqypcx7mrvdanx2ety9e3k7mgxqzjccqpjsp5v8eekpxchenfy2vwzr92nmlthzer32wsu075jhn3vgwvm8pj0ccqrzjqgz6hj9598d0davwpw8nrsak7nj6fltzjnt3ewh05fgx2rpv2xvekzn3auqqt2sqqqqqqqqqqqqqqeqq9q9qgsqqqyssq0t2caakcr9elrc8umpytj5v20fylfftxke8w4m5x7mauh4zy9yr8ls6h4rye6605heajcxewqfhacr0lyquhcg6qfyj99t2ld2gn5tcphvhhfp"
        ),
        callbackURL = None
      )
    }
    "insrt pending debit" in {
      for {
        Some(_) <- lightningPaymentsDAO.insert(pendingDebit)
        Some(fc) <- lightningPaymentsDAO.findByBolt11(pendingDebit.bolt11)
      } yield {
        fc.metadata shouldBe pendingDebit.metadata
        fc.label shouldBe pendingDebit.label
        fc.status shouldBe pendingDebit.status
        fc.payStatus shouldBe PayStatus.pending
        assert(fc.amount_msat === pendingDebit.amount_msat)
        fc.bolt11 shouldBe pendingDebit.bolt11
        fc.amount_sent_msat shouldBe pendingDebit.amount_sent_msat
        assert(fc.payment_hash === pendingDebit.payment_hash)
      }
    }
    "findPending" in {
      for {
        Some(_) <- lightningPaymentsDAO.insert(pendingDebit)
        Some(_) <- lightningPaymentsDAO.insert(
          pendingDebit
            .copy(status = PayStatus.complete.toString,
                  bolt11 = b2,
                  payment_hash = b2.invoice.paymentHash.toString,
                  label = SecureIdentifier(8).toString.some)
        )
        pendingDebits <- lightningPaymentsDAO.findPending
        c <- lightningPaymentsDAO.count

      } yield {
        pendingDebits.length shouldBe 1
        c shouldBe 2

      }
    }
    "compareToSchema" in {
      for {
        Some(_) <- lightningPaymentsDAO.insert(pendingDebit)
        Some(_) <- lightningPaymentsDAO.insert(
          pendingDebit
            .copy(status = PayStatus.complete.toString,
                  bolt11 = b2,
                  payment_hash = b2.invoice.paymentHash.toString,
                  label = SecureIdentifier(8).toString.some)
        )
        pendingDebits <- lightningPaymentsDAO.findPending
        c <- lightningPaymentsDAO.count
        v <- lightningInvoicesDAO.compareToSchema

      } yield {
        pendingDebits.length shouldBe 1
        c shouldBe 2
        assert(v.isRight)

      }
    }
  }
}
