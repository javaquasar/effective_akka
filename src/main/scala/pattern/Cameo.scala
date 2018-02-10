package pattern

import scala.concurrent.duration.DurationInt
import common._
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, actorRef2Scala}
import akka.event.LoggingReceive

object AccountBalanceResponseHandler {
  case object AccountRetrievalTimeout

  def props(originalSender: ActorRef): Props = {
    Props(new AccountBalanceResponseHandler(originalSender))
  }
}

class AccountBalanceResponseHandler(originalSender: ActorRef) extends Actor with ActorLogging {

  import AccountBalanceResponseHandler._
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
      log.debug("Timeout occurred")
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

  import context.dispatcher
  val timeoutMessager: Cancellable = context.system.scheduler.scheduleOnce(
    250 milliseconds, self, AccountRetrievalTimeout)
}

class AccountBalanceRetriever(savingsAccounts: ActorRef, checkingAccounts: ActorRef, moneyMarketAccounts: ActorRef) extends Actor {
  def receive = {
    case GetCustomerAccountBalances(id) =>
      val originalSender = sender
      val handler = context.actorOf(AccountBalanceResponseHandler.props(originalSender), "cameo-message-handler")
      savingsAccounts.tell(GetCustomerAccountBalances(id), handler)
      checkingAccounts.tell(GetCustomerAccountBalances(id), handler)
      moneyMarketAccounts.tell(GetCustomerAccountBalances(id), handler)
  }
}