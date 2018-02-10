package pattern

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import common._
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import akka.event.LoggingReceive

object AccountBalanceRetrieverFinal {
  case object AccountRetrievalTimeout
}

class AccountBalanceRetrieverFinal(savingsAccounts: ActorRef, checkingAccounts: ActorRef, moneyMarketAccounts: ActorRef) extends Actor with ActorLogging {
  import AccountBalanceRetrieverFinal._

  def receive = LoggingReceive {
    case GetCustomerAccountBalances(id) => {
      log.debug(s"Received GetCustomerAccountBalances for ID: $id from $sender")
      val originalSender = sender

      context.actorOf(Props(new Actor() {
        var checkingBalances, savingsBalances, mmBalances: Option[List[(Long, BigDecimal)]] = None
        def receive = LoggingReceive {
          case CheckingAccountBalances(balances) =>
            log.debug(s"Received checking account balances: $balances")
            checkingBalances = balances
            collectBalances()
          case SavingsAccountBalances(balances) =>
            log.debug(s"Received savings account balances: $balances")
            savingsBalances = balances
            collectBalances()
          case MoneyMarketAccountBalances(balances) =>
            log.debug(s"Received money market account balances: $balances")
            mmBalances = balances
            collectBalances()
          case AccountRetrievalTimeout =>
            sendResponseAndShutdown(AccountRetrievalTimeout)
        }

        def collectBalances(): Unit = (checkingBalances, savingsBalances, mmBalances) match {
          case (Some(c), Some(s), Some(m)) =>
            log.debug(s"Values received for all three account types")
            timeoutMessager.cancel
            sendResponseAndShutdown(AccountBalances(checkingBalances, savingsBalances, mmBalances))
          case _ =>
        }

        def sendResponseAndShutdown(response: Any): Unit = {
          originalSender ! response
          log.debug("Stopping context capturing actor")
          context.stop(self)
        }

        savingsAccounts ! GetCustomerAccountBalances(id)
        checkingAccounts ! GetCustomerAccountBalances(id)
        moneyMarketAccounts ! GetCustomerAccountBalances(id)

        import context.dispatcher
        val timeoutMessager: Cancellable = context.system.scheduler.scheduleOnce(250 milliseconds, self, AccountRetrievalTimeout)
      }))
    }
  }
}