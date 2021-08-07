package payments.services

import akka.http.scaladsl.util.FastFuture
import com.mathbot.pay.lightning._
import com.typesafe.scalalogging.StrictLogging
import payments.debits.DebitsDAO
import sttp.client3.Response

import scala.concurrent.{ExecutionContext, Future}

class DatabaseLightningService(service: LightningApiService, debitsDAO: DebitsDAO)(
    implicit ec: ExecutionContext
) extends LightningService
    with StrictLogging {

  def getAccessToken = service.getAccessToken

  def lightningPayment(debitRequest: LightningDebitRequest) =
    service.lightningPayment(debitRequest)
  def updateLightningDebit(bolt11: Bolt11) = {
    service
      .withdrawalInfo(bolt11)
      .flatMap(res => {
        if (res.isSuccess)
          res.body match {
            case Left(value) => FastFuture.successful(Left(s"Parsing error ${res.code} ${res.body} $value"))
            case Right(Pays(Seq(listPay))) =>
              debitsDAO.updateStatus(bolt11, listPay.status) map (
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

  override def listPays(l: ListPaysRequest): Future[Response[Either[LightningRequestError, Pays]]] = ???

  override def getInfo: Future[Response[Either[LightningRequestError, LightningNodeInfo]]] = ???

  override def pay(pay: Pay): Future[Response[Either[LightningRequestError, Payment]]] = ???

  override def listInvoices(l: ListInvoicesRequest): Future[Response[Either[LightningRequestError, Invoices]]] = ???

  override def waitAnyInvoice(w: WaitAnyInvoice): Future[Response[Either[LightningRequestError, ListInvoice]]] = ???

  override def listOffers(
      r: LightningListOffersRequest
  ): Future[Response[Either[LightningRequestError, Seq[LightningOffer]]]] = ???

  override def decodePay(r: Bolt11): Future[Response[Either[LightningRequestError, DecodePay]]] = ???

  override def createOffer(
      offerRequest: LightningOfferRequest
  ): Future[Response[Either[LightningRequestError, LightningOffer]]] = ???

  override def invoice(inv: LightningInvoice): Future[Response[Either[LightningRequestError, LightningCreateInvoice]]] =
    ???
}
