package net.petitviolet.ex.persistence.task.actor

import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import net.petitviolet.ex.persistence.task.model._

object TaskPersistentActor {
  def props = Props[TaskPersistentActor]
}

class TaskPersistentActor extends PersistentActor with ActorLogging {
  override def persistenceId: String = "task_list"

  private var state: State = State()

  /**
   * API for update state
   * this event should be persisted
   *
   * @param event
   */
  private def updateState(event: CommandEvent): Unit = {
    persist(event) {
      case Register(title) => state = state + Task(title)
      case Complete(task)  => state = state complete Task(task)
      case Undo(task)      => state = state undo Task(task)
      case Archive(task)   => state = state - Task(task)
      case _               => sys.error("Invalid Message")
    }
  }

  /**
   * API for finding records in state
   * this event should not be persisted
   *
   * @param event
   */
  private def executeQuery(event: QueryEvent): Unit = {
    if (recoveryRunning) log.info("now recovering")
    event match {
      case GetNotCompleted => sender() ! NotCompletedTasks(state.notCompleted)
      case GetAllTask      => sender() ! AllTasks(state.all)
    }
  }

  override def receiveRecover: Receive = {
    case evt: CommandEvent                 => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  override def receiveCommand: Receive = {
    case command: CommandEvent => updateState(command)
    case query: QueryEvent     => executeQuery(query)
    case Snapshot              => saveSnapshot(state)
    case Print                 => println(s"current => $state")
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
 * expect to be persisted
 */
sealed trait CommandEvent extends Any
case class Register(taskTitle: TaskTitle) extends AnyVal with CommandEvent
case class Complete(taskTitle: TaskTitle) extends AnyVal with CommandEvent
case class Undo(taskTitle: TaskTitle) extends AnyVal with CommandEvent
case class Archive(taskTitle: TaskTitle) extends AnyVal with CommandEvent

sealed trait QueryEvent
case object GetNotCompleted extends QueryEvent
case object GetAllTask extends QueryEvent

case class AllTasks(value: Seq[Task]) extends AnyVal
case class NotCompletedTasks(value: Seq[Task]) extends AnyVal

/**
 * State of Actor
 *
 * @param taskList
 */
private case class State(taskList: Seq[Task] = Nil) {
  def all = taskList

  def notCompleted = taskList.filter { _.state == TaskState.Todo }

  def completed = taskList.filter { _.state == TaskState.Completed }

  def +(task: Task): State = copy(task +: taskList)

  def -(task: Task): State = copy(taskList.filterNot(_ == task))

  def complete(task: Task): State = modifyState(_.copy(state = TaskState.Completed))(task)

  def undo(task: Task): State = modifyState(_.copy(state = TaskState.Todo))(task)

  private def modifyState(f: => Task => Task)(task: Task) =
    copy(taskList.map { t =>
      if (t == task) f(t)
      else t
    })
}
