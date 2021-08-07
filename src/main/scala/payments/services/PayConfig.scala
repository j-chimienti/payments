package payments.services

import com.typesafe.config.Config

/**
 * @param baseUrl url of the bitcoin debits (pay app) e.g. https://btcdebits.mathbot.com
 * @param clientId
 * @param clientSecret
 */
case class PayConfig(baseUrl: String, clientId: String, clientSecret: String)

object PayConfig {
  def forConfig(config: Config): PayConfig = PayConfig(
    baseUrl = config.getString("pay.baseUrl"), clientId = config.getString("pay.clientId"), clientSecret = config.getString("pay.clientSecret")
  )
}
