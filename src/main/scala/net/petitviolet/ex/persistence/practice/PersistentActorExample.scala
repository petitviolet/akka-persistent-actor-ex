package net.petitviolet.ex.persistence.practice

import akka.actor._
import akka.persistence._

// commands
private sealed trait MyCommand
private case object MySnapshot extends MyCommand
private case object MyPrint extends MyCommand
private case class AppendCommand(data: Int) extends MyCommand

// states
private case class MyState(events: Seq[Int] = Seq.empty) {
  def updated(evt: AppendCommand) = copy(evt.data +: events)

  def state = toString

  override def toString: String = events.reverse.mkString(" :: ")
}

private class ExamplePersistentActor extends PersistentActor {
  override def persistenceId = "example-id"

  private var state = MyState()

  override def receiveRecover: Receive = {
    case command: AppendCommand =>
      println(s"command: $command")
      state = state.updated(command)
    case SnapshotOffer(_, snapshot: MyState) =>
      state = snapshot
    case default => println(s"default: $default")
  }

  override def receiveCommand: Receive = {
    case command: AppendCommand =>
      persist(command) { _command =>
        state = state.updated(_command)
      }
    case MySnapshot => saveSnapshot(state)
    case MyPrint    => println(state.state)
  }

}

object PersistentActorExample extends App {

  val system = ActorSystem("PersistentActorExample")
  val persistentActor = system.actorOf(Props[ExamplePersistentActor], "my-example")

  persistentActor ! AppendCommand(-1)
  persistentActor ! MySnapshot
  persistentActor ! AppendCommand(3)
  persistentActor ! MyPrint

  Thread.sleep(1000)
  system.terminate()
}
