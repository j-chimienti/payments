package payments.services

import akka.http.scaladsl.util.FastFuture
import cats.data.{EitherT, OptionT}
import com.mathbot.pay.lightning.PayService.PlayerPayment__IN
import com.mathbot.pay.lightning._
import com.mathbot.pay.lightning.url.{CreateInvoiceWithDescriptionHash, InvoiceWithDescriptionHash}
import com.typesafe.scalalogging.StrictLogging
import payments.debits.{LightningPayment, LightningPaymentsDAO}
import payments.lightninginvoices.LightningInvoicesDAO
import payments.models.LightningInvoiceModel
import sttp.client3

import scala.concurrent.{ExecutionContext, Future}

class DatabaseLightningService(
    service: LightningService,
    debitsDAO: LightningPaymentsDAO,
    lightningInvoicesDAO: LightningInvoicesDAO
)(implicit
  ec: ExecutionContext)
    extends StrictLogging {

  //////////////////// CREDITS ////////////////////

  // TODO: prevent 2nd api call
  def invoice(inv: LightningInvoice, playerAccountId: String): EitherT[Future, LightningRequestError, ListInvoice] =
    for {
      value <- EitherT(service.invoice(inv).map(_.body))
      li <- EitherT(service.getInvoiceByPaymentHash(payment_hash = value.payment_hash).map(_.body))
      j <- OptionT(lightningInvoicesDAO.insert(LightningInvoiceModel(li, playerAccountId)))
        .toRight(LightningRequestError(500, "Error inserting invoice"))
    } yield li

  def poll(payment_hash: String): Future[client3.Response[Either[LightningRequestError, ListInvoice]]] =
    for {
      value <- service.getInvoiceByPaymentHash(payment_hash)
      _ <- value.body match {
        case Right(listInvoice) if listInvoice.isPaid || listInvoice.isExpired =>
          lightningInvoicesDAO.update(listInvoice)
        case _ =>
          FastFuture.successful("Skipping invoice updating")
      }
    } yield value

  def invoiceWithDescriptionHash(
      i: InvoiceWithDescriptionHash,
      playerAccountId: String
  ): EitherT[Future, String, (CreateInvoiceWithDescriptionHash, ListInvoice, LightningInvoiceModel)] = {
    for {
      b <- EitherT(
        service
          .invoiceWithDescriptionHash(i)
          .map(_.body.left.map(e => e.toString))
      )
      li <- EitherT(
        service
          .getInvoiceByPaymentHash(b.payment_hash)
          .map(_.body.left.map(e => e.toString))
      )
      invModel = LightningInvoiceModel(b, li, playerAccountId)
      _ <- OptionT(lightningInvoicesDAO.insert(invModel))
        .toRight("Unable to insert invoice")
    } yield (b, li, invModel)
  }

  def lightningPayment(debitRequest: PlayerPayment__IN) =
    for {
      // make sure does not exist
      _ <- EitherT(debitsDAO.findByBolt11(bolt11 = debitRequest.bolt11) map {
        case None => Right()
        case Some(duplicate) => Left("duplicate")
      })
      d = LightningPayment.pending(debitRequest)
      _ <- EitherT.liftF(debitsDAO.insert(d))
      // todo: use player payment
      r <- EitherT(service.pay(Pay(debitRequest.bolt11)).map(_.body.left.map(_.message)))
      _ <- OptionT(debitsDAO.updateOne(debitRequest.bolt11, r.status)).toRight(s"error updating debit")
    } yield r
  def updateLightningDebit(bolt11: Bolt11) =
    service
      .listPays(ListPaysRequest(bolt11))
      .flatMap(res => {
        if (res.isSuccess)
          res.body match {
            case Left(value) => FastFuture.successful(Left(s"Parsing error ${res.code} ${res.body} $value"))
            case Right(Pays(Seq(listPay))) =>
              debitsDAO.updateOne(bolt11, listPay) map (r => r.map(d => Right(d)).getOrElse(Left("Not found")))
            case _ =>
              logger.info(s"Update debit request status to ${PayStatus.failed}")
              debitsDAO.updateOne(bolt11 = bolt11, status = PayStatus.failed) map (_ => Left(s"Not found ${bolt11}"))
          } else {
          val msg = s"${res.code} received from pay server"
          logger.warn(msg)
          FastFuture.successful(Left(msg))
        }
      })

  def updateLightningDebits(bolt11: Bolt11) =
    for {
      lp <- EitherT(service.listPays(ListPaysRequest(bolt11)).map(_.body.left.map(_ => "error")))
      jj <- OptionT {
        lp match {
          case Pays(Seq(listPay)) =>
            debitsDAO.updateOne(bolt11, listPay)
          case _ =>
            logger.info(s"Update debit request status to ${PayStatus.failed}")
            debitsDAO.updateOne(bolt11, PayStatus.failed)
        }
      }.toRight("Error updating debit")
    } yield {
      (lp, jj)
    }

}
