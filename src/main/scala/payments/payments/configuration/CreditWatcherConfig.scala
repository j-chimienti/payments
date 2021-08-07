package payments.payments.configuration

import scala.concurrent.duration.FiniteDuration

case class CreditWatcherConfig(getPendingAndUnpaidCreditsInterval: FiniteDuration)
