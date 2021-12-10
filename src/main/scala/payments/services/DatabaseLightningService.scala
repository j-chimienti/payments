package payments.services

import akka.Done
import akka.http.scaladsl.util.FastFuture
import akka.stream.{Attributes, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import cats.data.{EitherT, NonEmptyList, OptionT}
import com.mathbot.pay.lightning._
import com.mathbot.pay.lightning.lightningcharge.{LightningChargeInvoice, LightningChargeInvoiceRequest, LightningChargeService}
import com.mathbot.pay.lightning.url.InvoiceWithDescriptionHash
import com.typesafe.scalalogging.StrictLogging
import payments.credits.{Credit, CreditsDAO}
import payments.debits.{Debit, DebitsDAO}
import payments.lightninginvoices.{LightningInvoiceModel, LightningInvoicesDAO}
import payments.models.ValidDebitRequest
import sttp.client3.Response

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DatabaseLightningService(service: LightningService,
                               debitsDAO: DebitsDAO,
                               lightningInvoicesDAO: LightningInvoicesDAO,
                               creditsDAO: CreditsDAO)(
                                implicit ec: ExecutionContext
                              ) extends StrictLogging {

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
      _ <- r match {
        case Left(_) => FastFuture.successful(None)
        case Right(Some(value)) =>
          value.status match {
            case LightningInvoiceStatus.unpaid => FastFuture.successful(None)
            case LightningInvoiceStatus.paid =>
              for {
                _ <- lightningInvoicesDAO.update(value)
                _ <- creditsDAO.upsert(value, playerAccountId)
              } yield Done.done()
            case LightningInvoiceStatus.expired =>
              lightningInvoicesDAO.update(value)
          }
        case Right(None) => FastFuture.successful(None)
      }
    } yield r
  }

  def lightningChargeInvoice(invoiceRequest: LightningChargeInvoiceRequest, playerAccountId: String)(
    lightningChargeService: LightningChargeService
  ): EitherT[Future, String, LightningChargeInvoice] =
    for {
      r <- EitherT(lightningChargeService.invoice(invoiceRequest).map(_.body.left.map(_.toString)))
      j <- OptionT(lightningInvoicesDAO.insert(LightningInvoiceModel(r, playerAccountId)))
        .toRight("Error")

    } yield r

  def updateInvoicesAndCredits()(implicit m: Materializer) = {
    for {
      i <- service.listInvoices()
      dbInvs <- i.body match {
        case Left(value) => FastFuture.successful("")
        case Right(value) =>
          logger.info(s"Found ${value.invoices.size} invoices")
          Source(value.invoices)
            .mapAsync(10)(
              i => {
                for {
                  a <- creditsDAO.update(i).andThen {
                    case Failure(exception) =>
                      logger.error(s"CREDIT error ${i.label} $exception")
                    case Success(value) =>
                      val msg = value.filter(v => v.getModifiedCount > 0).map(ur => s"updated ${i.label}")
                      msg foreach logger.info
                  }
                  // todo: insert invcoies if description contains some key (need to check player acocunt id)
                  b <- lightningInvoicesDAO.update(i).andThen {
                    case Failure(exception) =>
                      logger.error(s"INVOICE error ${i.label} $exception")
                    case Success(value) =>
                      val msg = value.filter(v => v.getModifiedCount > 0).map(ur => s"updated ${i.label}")
                      msg foreach logger.info
                  }
                } yield (a, b)

              }
            )
            .addAttributes(
              Attributes.logLevels(
                onElement = Attributes.LogLevels.Off,
                onFinish = Attributes.LogLevels.Info,
                onFailure = Attributes.LogLevels.Error))
            .runWith(Sink.seq)
      }
    } yield i
  }

  /**
   *
   * Note: run after migrating since find reqire all invoices
   * @return
   */
  def findMissingCredits(implicit m : Materializer) = {
    for {
      invoices <- lightningInvoicesDAO.findByStatus(LightningInvoiceStatus.paid)
      credits <- creditsDAO.find()
      c = invoices.map(_.paymentHash).toSet diff credits.map(_.paymentHash).toSet
      i = c.flatMap(hash => invoices.find(_.paymentHash == hash).map(i => Credit(i)))
      nel = NonEmptyList.fromList(i.toList)
      r <- nel match {
        case Some(value) =>
          logger.info(s"Found ${value.size} missing credits ${value.map(_.label)}")
          Source(value.toList)
            .grouped(1000)
            .map(l => NonEmptyList.fromList(l.toList))
            .collect { case Some(s) => s}
            .mapAsync(1)(v =>
          creditsDAO.insertMany(v)
            )    .addAttributes(
            Attributes.logLevels(
              onElement = Attributes.LogLevels.Off,
              onFinish = Attributes.LogLevels.Info,
              onFailure = Attributes.LogLevels.Error)).runWith(Sink.seq)
        case None => FastFuture.successful("")
      }
    } yield r
  }

  def invoiceWithDescriptionHash(i: InvoiceWithDescriptionHash, playerAccountId: String) = {
    for {
      b <- EitherT(
        service
          .invoiceWithDescriptionHash(i)
          .map(_.body.left.map(e => e.toString))
      )
      li <- EitherT(
        service
          .getInvoice(b.payment_hash)
          .map {
            case Left(value) => Left(value.toString)
            case Right(Some(v)) => Right(v)
            case Right(None) => Left("not found")
          }
      )
      invModel = LightningInvoiceModel(b, li, playerAccountId)
      _ <- OptionT(lightningInvoicesDAO.insert(invModel))
        .toRight("Unable to insert invoice")
    } yield (b, li, invModel)
  }

  //////////////////// DEBITS ////////////////////
  def checkDebits(implicit m: Materializer) =
    for {
      invoicesR <- service.listPays()
      _ <- invoicesR.body match {
        case Left(value) => FastFuture.successful(Seq.empty)
        case Right(value) =>
          Source(value.pays)
          // note: slow but calling debits can lead to codec errors
            .mapAsync(10)(debit => debitsDAO.updateOne(debit))

          .log(name = "checkDebits")
          .addAttributes(
            Attributes.logLevels(
              onElement = Attributes.LogLevels.Off,
              onFinish = Attributes.LogLevels.Info,
              onFailure = Attributes.LogLevels.Error))
            .runWith(Sink.seq)
      }
    } yield invoicesR

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
    service
      .listPays(ListPaysRequest(bolt11))
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

  //// run all
  def migrate(implicit m: Materializer) =
    for {
      r <- checkDebits
      r1 <- updateInvoicesAndCredits
      r2 <- findMissingCredits(m)
    } yield (r, r1, r2)

}
