package payments.payments.pay

/**
 * @param baseUrl url of the bitcoin debits (pay app) e.g. https://btcdebits.mathbot.com
 * @param clientId
 * @param clientSecret
 */
case class PayConfig(baseUrl: String, clientId: String, clientSecret: String)
