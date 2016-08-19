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
  case object Save extends ShoppingEvent

  case class ShoppingData private[ShoppingData] (items: Set[Item])
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
    println(s"price => ${data.items.map(_.price).sum}")

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
      case Save =>
        saveStateSnapshot()
        currentData
    }
  }

  // initial state and data
  startWith(Looking, ShoppingData.empty)

  override def onRecoveryCompleted(): Unit = {
    log.info(s"recovery completed: $stateData")
  }

  // extended `goto` with logging
  private def gotoLogging(state: ShoppingState) = {
    log.debug(s"\n***state => $state, current: $stateName")
    super.goto(state)
  }

  when(Looking) {
    case Event(Save, _)             => stay applying Save
    case Event(addItem: AddItem, _) => gotoLogging(Shopping) applying addItem
  }

  when(Shopping) {
    case Event(Save, _)                   => stay applying Save
    case Event(addItem: AddItem, _)       => stay applying addItem
    case Event(removeItem: RemoveItem, _) => stay applying removeItem
    case Event(Purchase, _)               => gotoLogging(Purchased) applying Purchase
  }

  when(Purchased) {
    case Event(Save, _)  => stay applying Save
    case Event(Leave, _) => gotoLogging(Looking) applying Leave
  }

}

private object PersistFSMApp extends App {

  implicit val system = ActorSystem("PersistFSMApp")
  val actor = system.actorOf(Props(classOf[Customer], classTag[ShoppingEvent]))

  val milk = Item("milk", 100)
  val meat = Item("meat", 1000)

  actor ! AddItem(milk)
  actor ! AddItem(meat)
  actor ! RemoveItem(milk)

  //  actor ! Purchase
  //  actor ! Leave

  val rand = Item("mystery", Random.nextInt(300))
  actor ! AddItem(rand)

  actor ! Save
  actor ! AddItem(milk)

  Thread.sleep(1000)
  system.terminate()
}
