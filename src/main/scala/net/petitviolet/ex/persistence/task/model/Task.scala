package net.petitviolet.ex.persistence.task.model

case class Task(title: TaskTitle, state: TaskState = TaskState.Todo)

case class TaskTitle(value: String)

sealed abstract class TaskState(val n: Int) extends Serializable

object TaskState {
  private val values = Completed :: Todo :: Nil

  def from(n: Int): TaskState = values.find(_.n == n).get

  case object Completed extends TaskState(1)
  case object Todo extends TaskState(0)
}

