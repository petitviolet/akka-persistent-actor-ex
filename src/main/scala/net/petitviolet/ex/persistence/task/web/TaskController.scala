package net.petitviolet.ex.persistence.task.web

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import net.petitviolet.ex.persistence.task.actor.{Register, AllTasks, GetAllTask, TaskPersistentActor}
import net.petitviolet.ex.persistence.task.model.TaskJsonSupport._
import net.petitviolet.ex.persistence.task.model.{TaskTitle, Task}
import spray.json.{RootJsonFormat, DefaultJsonProtocol}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait UsingTaskActor extends UsesAppContext {
  import appContext._
  protected implicit val timeout = Timeout(1 second)
  protected val actor = system.actorOf(TaskPersistentActor.props)
}

class TaskController(implicit val appContext: AppContext) extends JsonController {
  override val route: Route = new GetTaskController().route ~ new RegisterTaskController().route
}

class GetTaskController(implicit val appContext: AppContext) extends JsonController with UsingTaskActor {
  import appContext._

  override val route: Route = (path("task" / "all") & get) {
    val tasks: Future[Seq[Task]] = (actor ? GetAllTask).mapTo[AllTasks].map { _.value }
    completeFuture(tasks)
  }

}

class RegisterTaskController(implicit val appContext: AppContext) extends JsonController with UsingTaskActor {
  import appContext._
  import ResultJsonSupport._
  import RegisterTaskDTOJsonSupport._

  override val route: Route = (path("task" / "new") & post) {
    entity(as[RegisterTaskDTO]) { dto =>
      actor ! Register(TaskTitle(dto.title))
      complete(Result("ok"))
    }
  }

}

private case class RegisterTaskDTO(title: String)
private case class Result(result: String)

private object RegisterTaskDTOJsonSupport extends DefaultJsonProtocol {
  implicit val registerTaskFormat: RootJsonFormat[RegisterTaskDTO] = jsonFormat1(RegisterTaskDTO)
}

private object ResultJsonSupport extends DefaultJsonProtocol {
  implicit val resultFormat: RootJsonFormat[Result] = jsonFormat1(Result)
}
