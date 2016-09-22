package net.petitviolet.ex.persistence.task.actor

import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import net.petitviolet.ex.persistence.task.model.TaskState.{ Todo, Completed }
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
      case Register(task, _, _) => state = state + task
      case Complete(task)       => state = state complete task
      case Undo(task)           => state = state undo task
      case Archive(task)        => state = state - task
      case _                    => sys.error("Invalid Message")
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
      case GetCompleted    => sender() ! CompletedTasks(state.completed)
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
case class Register(task: Task, flag: Boolean = false, id: Int = 1) extends CommandEvent
case class Complete(taskId: TaskId) extends CommandEvent
case class Undo(taskId: TaskId) extends CommandEvent
case class Archive(taskId: TaskId) extends CommandEvent

sealed trait QueryEvent
case object GetNotCompleted extends QueryEvent
case object GetCompleted extends QueryEvent
case object GetAllTask extends QueryEvent
object QueryEvent {
  def byState(taskState: TaskState): QueryEvent =
    taskState match {
      case Completed => GetCompleted
      case Todo      => GetNotCompleted
    }
}

sealed trait TaskList extends Any {
  val value: Seq[Task]
}
case class AllTasks(value: Seq[Task]) extends AnyVal with TaskList
case class NotCompletedTasks(value: Seq[Task]) extends AnyVal with TaskList
case class CompletedTasks(value: Seq[Task]) extends AnyVal with TaskList

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

  def -(taskId: TaskId): State = copy(taskList.filterNot(_ == taskId))

  def complete(taskId: TaskId): State = modifyState(_.copy(state = TaskState.Completed))(taskId)

  def undo(taskId: TaskId): State = modifyState(_.copy(state = TaskState.Todo))(taskId)

  private def modifyState(f: => Task => Task)(taskId: TaskId) =
    copy(taskList.map { t =>
      if (t.id == taskId) f(t)
      else t
    })
}
