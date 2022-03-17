package payments.services

import akka.http.scaladsl.model.Uri
import com.github.dwickern.macros.NameOf.nameOf
import com.mathbot.pay.lightning.{Response => _, _}
import play.api.libs.json.{JsError, JsSuccess, Json}
import sttp.capabilities.akka.AkkaStreams
import sttp.client3.SttpBackend
import sttp.model.MediaType

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

case class AccessToken(access_token: String, expires_in: Int, scope: String, token_type: String)
object AccessToken {
  implicit val formatAccessToken = Json.format[AccessToken]
}

class LightningApiService(backend: SttpBackend[Future, AkkaStreams], config: PayConfig)(
    implicit ec: ExecutionContext
) {
  import sttp.client3._
  import playJson._
  private val btcEndpoint: Uri = config.baseUrl
  private val clientId = config.clientId
  private val clientSecret = config.clientSecret

  private var accessToken: String = "INVALID_ACCESS_TOKEN"

  def getAccessToken =
    basicRequest
      .post(uri"$btcEndpoint/oauth2/token")
      .auth
      .basic(clientId, clientSecret)
      .response(asJson[AccessToken])
      .send(backend)
      .map(r => {
        r.body match {
          case Right(at) =>
            accessToken = at.access_token
          case Left(err) =>
        }
        r
      })

  def lightningPayment(
      debitRequest: LightningDebitRequest
  ) = {
    val req = basicRequest
      .post(uri"$btcEndpoint/lightning/pay")
      .contentType(MediaType.ApplicationJson)
      .body(Json.toJson(debitRequest).toString)
      .readTimeout(Duration("2m")) // ln payments can take 60 seconds
      .response(
        asStringAlways
          .map { s =>
            val j = Json.parse(s)
            j.validate[Payment] match {
              case JsSuccess(p, _) => Right(p)
              case JsError(e) =>
                j.validate[LightningRequestError] match {
                  case JsSuccess(value, _) => Left(value)
                  case JsError(e) => Left(LightningRequestError(ErrorMsg(500, e.mkString(","))))
                }
            }
          }
      )
    req.send(backend)
  }

  def withdrawalInfo(bolt11: Bolt11) = {
    val m = Map("bolt11" -> bolt11.toString)
    val req = basicRequest
      .get(uri"$btcEndpoint/lightning/pay?$m")
      .response(asString.map(r => {
        r.map(s => {
            val jsonBody = Json.parse(s)
            jsonBody.asOpt[Pays] match {
              case Some(p) => Right(p)
              case _ =>
                jsonBody.asOpt[LightningRequestError] match {
                  case Some(e) => Left(e)
                  case None =>
                    Left(
                      new LightningRequestError(ErrorMsg(500, s"Not a ${nameOf(LightningRequestError)} $jsonBody"))
                    )
                }
            }
          })
          .getOrElse {
            Left(LightningRequestError(ErrorMsg(500, s"Not a ${nameOf(LightningRequestError)} $r")))
          }
      }))
    req.send(backend)
  }

}
