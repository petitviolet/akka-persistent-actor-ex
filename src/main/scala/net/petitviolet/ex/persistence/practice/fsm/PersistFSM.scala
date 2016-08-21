package net.petitviolet.ex.persistence.practice.fsm

import akka.actor.{ ActorSystem, Props }
import akka.persistence.fsm.PersistentFSM
import net.petitviolet.ex.persistence.practice.fsm.ForFSM._

import scala.reflect.ClassTag
import scala.reflect._
import scala.util.Random

private object ForFSM {
  case class Item(name: String, price: Int)

  sealed trait ShoppingEvent
  case class AddItem(item: Item) extends ShoppingEvent
  case class RemoveItem(item: Item) extends ShoppingEvent
  case object Purchase extends ShoppingEvent
  case object Leave extends ShoppingEvent

  case class ShoppingData private[ShoppingData] (items: Set[Item]) {
    def price: Int = items.map(_.price).sum
  }
  object ShoppingData {
    val empty = ShoppingData(Set.empty)
  }

  sealed trait ShoppingState extends PersistentFSM.FSMState {
    override def identifier: String = s"my-event: ${getClass.getSimpleName}"
  }
  case object Looking extends ShoppingState
  case object Shopping extends ShoppingState
  case object Purchased extends ShoppingState
}

private class Customer(implicit val domainEventClassTag: ClassTag[ShoppingEvent])
  extends PersistentFSM[ShoppingState, ShoppingData, ShoppingEvent] {

  override def persistenceId: String = "example-persistence-FSM"

  private def purchase(data: ShoppingData): Unit =
    println(s"price => ${data.price}")

  override def applyEvent(domainEvent: ShoppingEvent, currentData: ShoppingData): ShoppingData = {
    log.info(s"\n***applyEvent: $domainEvent, $currentData, state: $stateName")
    domainEvent match {
      case AddItem(item) =>
        currentData.copy(items = currentData.items + item)
      case RemoveItem(item) =>
        currentData.copy(items = currentData.items - item)
      case Purchase =>
        purchase(currentData)
        ShoppingData.empty
      case Leave =>
        ShoppingData.empty
    }
  }

  override def onRecoveryCompleted(): Unit = {
    log.info(s"recovery completed: $stateData")
  }

  // initial state and data
  startWith(Looking, ShoppingData.empty)

  // extended `goto` with logging
  private def gotoLogging(state: ShoppingState) = {
    log.info(s"\ngoto $stateName => $state")
    super.goto(state)
  }

  // extended `stay` with logging
  private def stayLogging = {
    log.info(s"\nstay $stateName")
    super.stay
  }

  when(Looking) {
    case Event(addItem: AddItem, _) =>
      // start shopping with adding an item
      gotoLogging(Shopping) applying addItem andThen {
        case afterAddItem => log.info(s"after => $afterAddItem")
      }
  }

  when(Shopping) {
    case Event(addItem: AddItem, _) =>
      // just add an item
      stayLogging applying addItem

    case Event(removeItem: RemoveItem, ShoppingData.empty) =>
      // if currentData is empty, disallowed RemoveItem
      throw new IllegalStateException("ShoppingCart is Empty")
    case Event(removeItem @ RemoveItem(item), ShoppingData(items)) if items.size == 1 && (items contains item) =>
      // if currentData will be removed, goto Looking state
      gotoLogging(Looking) applying removeItem
    case Event(removeItem: RemoveItem, _) =>
      // general behavior, stay and remove
      stayLogging applying removeItem

    case Event(Purchase, _) =>
      // go to cashier and having payment
      gotoLogging(Purchased) applying Purchase
  }

  when(Purchased) {
    case Event(Leave, _) =>
      // leave from the shop
      gotoLogging(Looking) applying Leave
  }

  onTransition {
    case Purchased -> Looking =>
      // take snapshot when shopping has completed
      log.info(s"Save SnapShot")
      saveStateSnapshot()
  }

}

private object PersistFSMApp extends App {

  implicit val system = ActorSystem("PersistFSMApp")
  val actor = system.actorOf(Props(classOf[Customer], classTag[ShoppingEvent]))

  val milk = Item("milk", 100)
  val meat = Item("meat", 1000)

  actor ! AddItem(milk)
  actor ! RemoveItem(milk)
  actor ! AddItem(meat)

  //  actor ! Purchase
  //  actor ! Leave
  //
  val rand = Item("mystery", Random.nextInt(300))
  actor ! AddItem(rand)
  //
  //  //  actor ! Save
  //  actor ! AddItem(milk)

  Thread.sleep(1000)
  system.terminate()
}
