object AccountActor { 
  case class Deposit(sum: Long)
  case class Withdraw(sum: Long)
}

class AccountActor extends Actor with ActorLogging {
  var accountSum = 0L
  def receive = {
    case Deposit(sum) => 
      accountSum += sum
      logStatus
    case Withdraw(sum) => 
      if (sum <= accountSum) accountSum -= sum
      logStatus
  }
  
  def logStatus = log.info(s"Account sum is: $accountSum")
}

val system = ActorSystem("ActorTest")
val account1 = system.actorOf(Props[AccountActor], "account1")
account1 ! Deposit(100L)
account1 ! Withdraw(200L)
account1 ! Withdraw(99L)
