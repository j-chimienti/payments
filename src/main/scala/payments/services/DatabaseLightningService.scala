package payments.services

import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import cats.data.NonEmptyList
import com.mathbot.pay.lightning._
import com.mathbot.pay.lightning.url.{CreateInvoiceWithDescriptionHash, InvoiceWithDescriptionHash}
import com.typesafe.scalalogging.StrictLogging
import payments.credits.{Credit, CreditsDAO}
import payments.debits.{Debit, DebitsDAO}
import payments.lightninginvoices.{LightningInvoiceModel, LightningInvoicesDAO}
import payments.models.ValidDebitRequest
import sttp.client3.Response

import scala.concurrent.{ExecutionContext, Future}

class DatabaseLightningService private (service: LightningService, debitsDAO: DebitsDAO, lightningInvoicesDAO: LightningInvoicesDAO, creditsDAO: CreditsDAO)(
  implicit ec: ExecutionContext
)
  extends StrictLogging {


  //////////////////// CREDITS ////////////////////

  def invoice(inv: LightningInvoice, playerAccountId: String) =
    for {
    r <- service.invoice(inv)
    _ <- r.body match {
      case Left(value) => FastFuture.successful(())
      case Right(value) =>
        lightningInvoicesDAO.insert(LightningInvoiceModel(value, inv, playerAccountId))
    }
    } yield r

  def poll(payment_hash: String, playerAccountId: String) = {
    for {
    r <- service.getInvoice(payment_hash)
    j <- r match {
      case Left(value) => FastFuture.successful(None)
      case Right(Some(value)) =>
        value.status match {
          case LightningInvoiceStatus.unpaid => FastFuture.successful(None)
          case  LightningInvoiceStatus.paid =>
            for {
            _ <- lightningInvoicesDAO.update(value)
            _ <- creditsDAO.insert(Credit(value, playerAccountId))
            } yield ()
          case LightningInvoiceStatus.expired  =>
           lightningInvoicesDAO.update(value)
        }
      case Right(None) => FastFuture.successful(None)
    }
    } yield r
  }

  def checkInvoicesStatus()(implicit m: Materializer)  = {
    for {
    i <- service.listInvoices()
    dbInvs <- i.body match {
      case Left(value) => FastFuture.successful("")
      case Right(value) =>
        logger.info(s"Found ${value.invoices.size} invoices")
        Source(value.invoices)
          .mapAsync(10)(
           i => lightningInvoicesDAO.update(i)
          ).runWith(Sink.seq)
    }
    } yield i
  }

  def findMissingCredits() = {
    for {
    invoices <- lightningInvoicesDAO.findByStatus(LightningInvoiceStatus.paid)
    credits <- creditsDAO.find()
    c = credits.flatMap(c => {
      invoices.find(_.paymentHash == c.paymentHash).map(i =>
        Credit(i, c.playerAccountId)
      )
    })
    nel = NonEmptyList.fromList(c.toList)
    r <- nel match {
      case Some(value) => creditsDAO.insertMany(value)
      case None => FastFuture.successful("")
    }
    } yield r
  }

  //////////////////// DEBITS ////////////////////

  def checkDebits(implicit m: Materializer) = {
    for {
    invoicesR <- service.listPays()
        debits <- debitsDAO.find()
    debits <- invoicesR.body match {
      case Left(value) => FastFuture.successful(Seq.empty)
      case Right(value) =>
        val d = debits.flatMap(d => value.pays.find(p => d.paymentHash.exists(p.payment_hash.contains) || p.bolt11.contains(d.bolt11))
          .map(lp => (d, lp))
        )
        Source(d)
        .mapAsync(10)(debit => debitsDAO.updateOne(debit._2))
        .runWith(Sink.seq)
    }
    } yield invoicesR
  }


  def lightningPayment(debitRequest: ValidDebitRequest): Future[Response[Either[LightningRequestError, Payment]]] =
    for {
      debitOpt <- debitsDAO.find(bolt11 = debitRequest.pay.bolt11)
       d <- debitOpt match {
        case Some(value) => FastFuture.successful(value)
        case None =>
          val d = Debit(debitRequest)
          debitsDAO.insert(d).map(_ => d)
      }
    r <- service.pay(debitRequest.pay)
    _ <- r.body match {
      case Left(value) => FastFuture.successful(None)
      case Right(value) => debitsDAO.updateOne(value, debitRequest)
    }
    } yield r
  def updateLightningDebit(bolt11: Bolt11): Future[Either[String, Debit]] =
      service.listPays(ListPaysRequest(bolt11))
      .flatMap(res => {
        if (res.isSuccess)
          res.body match {
            case Left(value) => FastFuture.successful(Left(s"Parsing error ${res.code} ${res.body} $value"))
            case Right(Pays(Seq(listPay))) =>
              debitsDAO.updateOne(listPay) map (
                r => r.map(d => Right(d)).getOrElse(Left("Not found"))
                )
            case _ =>
              logger.info(s"Update debit request status to ${PayStatus.failed}")
              debitsDAO.updateStatus(bolt11, PayStatus.failed) map (
                _ => Left(s"Not found ${bolt11}")
                )
          } else {
          val msg = s"${res.code} received from pay server"
          logger.warn(msg)
          FastFuture.successful(Left(msg))
        }
      })

}
