package payments.lightninginvoices

import com.mathbot.pay.lightning.{Bolt11, LightningInvoiceStatus, ListInvoice}
import fr.acinq.eclair.MilliSatoshi
import org.mongodb.scala.MongoWriteException
import payments.DatabaseSuite
import payments.models.LightningInvoiceModel

import java.time.Instant

class LightningInvoicesDAOTest extends DatabaseSuite {

  val inv = ListInvoice(
    label = "test",
    bolt11 = Some(
      Bolt11(
        "lnbc100n1psnwh5spp53p4z3vw0n2qgfl6j9j5dyxk8vkzgm74g0jyfr8w4c3t65h4teyrqdp0gejk2epqgd5xjcmtv4h8xgzqypcx7mrvdanx2ety9e3k7mgxqzjccqpjsp5jyeuh8s8527pw3sulnmylrhzmvgtrx4fuqgwhzt99cyhfqmhevdqrzjqt5sge2449n9z3dsm0tlzdt5gkvyrr0h6cwnvcr9jeqcsmh3yayygz5puvqqnhqqqqqqqqryqqqq86qqxg9qgsqqqyssq2a7pnht9ykkjpq3ck0nehsy433r7xv52nms2wkuuukfy8x7ckjv5a5ng2vyugcggktk7h5epuvnmffznek0m6yvvtvna6r6v7nlla8spqwsqa0"
      )
    ),
    payment_hash = "886a28b1cf9a8084ff522ca8d21ac765848dfaa87c88919dd5c457aa5eabc906",
    amount_msat = Some(MilliSatoshi(10000)),
    status = LightningInvoiceStatus.unpaid,
    pay_index = Some(15018),
    amount_received_msat = Some(MilliSatoshi(10000)),
    paid_at = Some(Instant.now),
    description = "desc",
    expires_at = Instant.now,
    bolt12 = None,
    local_offer_id = None,
    payer_note = None,
    payment_preimage = None
  )
  val l = LightningInvoiceModel.apply(inv, "pid")

  "count (init)" in {
    for {
      r <- lightningInvoicesDAO.count
    } yield {
      assert(r === 0)
    }
  }
  "insert" in {
    for {
      r <- lightningInvoicesDAO.insert(l)
      c <- lightningInvoicesDAO.count
    } yield {
      assert(r.isDefined)
      assert(c === 1)
    }
  }
  "insert fails with duplicate bolt11" in {
    for {
      _ <- lightningInvoicesDAO.insert(l)
      r <- recoverToExceptionIf[MongoWriteException] {
        lightningInvoicesDAO.insert(l.copy(label = "new label"))
      }
    } yield {
      assert(r.getError.getCode === 11000)
    }
  }
  "insert fails with duplicate label" in {
    for {
      _ <- lightningInvoicesDAO.insert(l)
      r <- recoverToExceptionIf[MongoWriteException] {
        lightningInvoicesDAO.insert(
          l.copy(
            bolt11 = Bolt11(
              "lnbc22435140n1p0zxgcupp5hkn3lnlkk6kmq0670a9u2xd8reuefc7dw704pwe7dqqm06nnzpcsdz2gf5hgun9ve5kcmpqv9jx2v3e8pjkgtf5xaskxtf5venxvttp8pjrvttrvgerzvm9x93r2dehxgfppjue4tflpg2hule862xylcsnu0p0mrjvmnxqrp9s2xnqfz900f0mq36dnkseqped68nzjm2nvh85uxw4nkhezkd4zzlymw93q96mf9glupqxrfd46rps7dztqm5rerusxpx77curwrpal9qqenm4p0"
            )
          )
        )
      }
    } yield {
      assert(r.getError.getCode === 11000)
    }
  }
  "insert works with duplicate payment_hash" in {
    val updated = l.copy(
      label = "new label",
      bolt11 = Bolt11(
        "lnbc22435140n1p0zxgcupp5hkn3lnlkk6kmq0670a9u2xd8reuefc7dw704pwe7dqqm06nnzpcsdz2gf5hgun9ve5kcmpqv9jx2v3e8pjkgtf5xaskxtf5venxvttp8pjrvttrvgerzvm9x93r2dehxgfppjue4tflpg2hule862xylcsnu0p0mrjvmnxqrp9s2xnqfz900f0mq36dnkseqped68nzjm2nvh85uxw4nkhezkd4zzlymw93q96mf9glupqxrfd46rps7dztqm5rerusxpx77curwrpal9qqenm4p0"
      ),
      payment_hash = l.payment_hash
    )
    for {
      c0 <- lightningInvoicesDAO.count
      _ <- lightningInvoicesDAO.insert(l)
      c1 <- lightningInvoicesDAO.count
      r <- lightningInvoicesDAO.insert(updated)
      c2 <- lightningInvoicesDAO.count
    } yield {
      assert(c0 == 0)
      assert(c1 == 1)
      assert(c2 == 2)
    }
  }
  "insert works with non duplicate label,bolt11,payment_hash" in {
    for {
      _ <- lightningInvoicesDAO.insert(l)
      r <- lightningInvoicesDAO.insert(
        l.copy(
          label = "new label",
          bolt11 = Bolt11(
            "lnbc22435140n1p0zxgcupp5hkn3lnlkk6kmq0670a9u2xd8reuefc7dw704pwe7dqqm06nnzpcsdz2gf5hgun9ve5kcmpqv9jx2v3e8pjkgtf5xaskxtf5venxvttp8pjrvttrvgerzvm9x93r2dehxgfppjue4tflpg2hule862xylcsnu0p0mrjvmnxqrp9s2xnqfz900f0mq36dnkseqped68nzjm2nvh85uxw4nkhezkd4zzlymw93q96mf9glupqxrfd46rps7dztqm5rerusxpx77curwrpal9qqenm4p0"
          ),
          payment_hash = "0" * 64
        )
      )
      c <- lightningInvoicesDAO.count
    } yield {
      assert(r.isDefined)
      c shouldBe 2
    }
  }
  "update listpay info" in {
    assert(l.invoiceStatus == LightningInvoiceStatus.unpaid)
    for {
      _ <- lightningInvoicesDAO.insert(l)
      r <- lightningInvoicesDAO.update(
        inv.copy(status = LightningInvoiceStatus.expired)
      )
      c <- lightningInvoicesDAO.findByBolt11(l.bolt11)
    } yield {
      assert(r.isDefined)
      assert(c.value.invoiceStatus === LightningInvoiceStatus.expired)
    }
  }

  "findByBolt11" in {
    for {
      _ <- lightningInvoicesDAO.insert(l)
      r <- lightningInvoicesDAO.findByBolt11(l.bolt11)
    } yield {
      assert(r.isDefined)
    }
  }

  "compareToSchema" in {
    for {
      _ <- lightningInvoicesDAO.insert(l)
      r <- lightningInvoicesDAO.findByBolt11(l.bolt11)
      c <- lightningInvoicesDAO.compareToSchema
    } yield {
      assert(r.isDefined)
      assert(c.isRight)
    }
  }

}
