package net.petitviolet.ex.persistence.task.model

import spray.json._

case class Task(title: TaskTitle, state: TaskState = TaskState.Todo)

case class TaskTitle(value: String)

sealed abstract class TaskState(val value: Int) extends Serializable

object TaskState {
  private val values = Completed :: Todo :: Nil

  def from(n: Int): TaskState = values.find(_.value == n).get

  case object Completed extends TaskState(1)
  case object Todo extends TaskState(0)
}


object TaskJsonSupport extends DefaultJsonProtocol {
  implicit val taskJsonFormat: RootJsonFormat[Task] = new RootJsonFormat[Task] {
    override def write(obj: Task): JsValue = JsObject(
      "title" -> JsString(obj.title.value),
      "state" -> JsNumber(obj.state.value)
    )

    override def read(json: JsValue): Task =
    json.asJsObject.getFields("title", "state") match {
      case Seq(JsString(title), JsNumber(state)) =>
        Task(TaskTitle(title), TaskState.from(state.toInt))
      case _ => throw new DeserializationException("Task")
    }
  }
}
