package net.petitviolet.ex.persistence

//#persistent-actor-example
import akka.actor._
import akka.persistence._
import net.petitviolet.ex.persistence.task.actor.GetNotCompleted

private sealed trait Command
private case object Snapshot extends Command
private case object Print extends Command
private case class ChangeCountCommand(data: Int) extends Command

private sealed trait Event
private case class IncreaseEvent(data: Int) extends Event
private case class DecreaseEvent(data: Int) extends Event

private sealed trait State
private case class CountState(events: Seq[Int] = Seq.empty) extends State {
  def updated(evt: Event) = evt match {
    case e: IncreaseEvent => updatedInc(e)
    case e: DecreaseEvent => updatedDec(e)
  }

  private def updatedInc(evt: IncreaseEvent): CountState = copy(evt.data +: events)
  private def updatedDec(evt: DecreaseEvent): CountState = copy(-evt.data +: events)

  def size: Int = events.length

  def state = events.sum

  override def toString: String = events.reverse.toString
}

class ExamplePersistentActor extends PersistentActor {
  override def persistenceId = "example-id"

  private var state = CountState()

  def updateState(event: Event): Unit =
    state = state.updated(event)

  def numEvents = state.size

  val receiveRecover: Receive = {
    case evt: Event                             => updateState(evt)
    case SnapshotOffer(_, snapshot: CountState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case ChangeCountCommand(num) =>
      val event: Event =
        if (num >= 0) IncreaseEvent(num)
        else DecreaseEvent(-num)
      persist(event)(updateState)
    case Snapshot => saveSnapshot(state)
    case Print    => println(state.state)
  }

}
//#persistent-actor-example

object PersistentActorExample extends App {

  val system = ActorSystem("PersistentActorExample")
  val persistentActor = system.actorOf(Props[ExamplePersistentActor], "persistentActor-4-scala")

  persistentActor ! ChangeCountCommand(-1)
  persistentActor ! ChangeCountCommand(8)
  persistentActor ! ChangeCountCommand(-2)
  persistentActor ! Snapshot
  persistentActor ! ChangeCountCommand(5)
  persistentActor ! GetNotCompleted

  Thread.sleep(1000)
  system.terminate()
}
