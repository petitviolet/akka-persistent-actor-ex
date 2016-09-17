package net.petitviolet.ex.persistence.task.model

import java.util.UUID

import spray.json._

import scala.language.reflectiveCalls

case class Task(id: TaskId, name: TaskTitle, state: TaskState = TaskState.Todo)

case class TaskId(value: String) extends AnyVal
case class TaskTitle(value: String) extends AnyVal

sealed abstract class TaskState(val value: Int) extends Serializable

object TaskId {
  def create: TaskId = {
    apply(UUID.randomUUID().toString.take(5))
  }
}
object TaskState {
  private val values = Completed :: Todo :: Nil

  //  def from(n: Int): TaskState = values.find(_.value == n).get
  def from: Int => TaskState = n => values.find(_.value == n).get

  case object Completed extends TaskState(1)
  case object Todo extends TaskState(0)
}

object TaskJsonSupport extends DefaultJsonProtocol {
  implicit val taskJsonFormat: RootJsonFormat[Task] = new RootJsonFormat[Task] {
    override def write(obj: Task): JsValue = JsObject(
      "id" -> JsString(obj.id.value),
      "title" -> JsString(obj.name.value),
      "state" -> JsNumber(obj.state.value)
    )

    override def read(json: JsValue): Task =
      json.asJsObject.getFields("title", "state") match {
        case Seq(JsString(id), JsString(title), JsNumber(state)) =>
          Task(TaskId(id), TaskTitle(title), TaskState.from(state.toInt))
        case _ => throw new DeserializationException("Task")
      }
  }
}

