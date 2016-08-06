package net.petitviolet.ex.persistence.task.actor

import akka.actor.Props
import akka.persistence.{PersistentActor, SnapshotOffer}
import net.petitviolet.ex.persistence.task.model._

object TaskPersistentActor {
  def props = Props[TaskPersistentActor]
}
class TaskPersistentActor extends PersistentActor {
  override def persistenceId: String = "task_list"

  private var state: State = State()

  private def updateState(event: Event): Unit = {
    event match {
      case Register(title) => state = state + Task(title)
      case Complete(task) => state = state complete task
      case Undo(task) => state = state undo task
      case Archive(task) => state = state - task
      case _ => sys.error("Invalid Message")
    }
  }

  override def receiveRecover: Receive = {
    case evt: Event                                 => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  override def receiveCommand: Receive = {
    case event: Event => updateState(event)
    case Snapshot  => saveSnapshot(state)
    case Print => println(s"current => $state")
  }

}

/**
  * Command to invoke some action on Actor
  */
sealed trait Command
case object Snapshot extends Command
case object Print extends Command

/**
  * Event for modify State of Actor
  */
sealed trait Event
case class Register(taskTitle: TaskTitle) extends Event
case class Complete(task: Task) extends Event
case class Undo(task: Task) extends Event
case class Archive(task: Task) extends Event

/**
  * State of Actor
  * @param taskList
  */
case class State(taskList: Seq[Task] = Nil) {
  def +(task: Task): State = copy(task +: taskList)

  def -(task: Task): State = copy(taskList.filterNot(_ == task))

  def complete(task: Task): State = modifyState(_.copy(state = Completed))(task)

  def undo(task: Task): State = modifyState(_.copy(state = Todo))(task)

  private def modifyState(f: => Task => Task)(task: Task) =
    copy(taskList.map { t =>
    if (t == task) f(t)
    else t
  })
}