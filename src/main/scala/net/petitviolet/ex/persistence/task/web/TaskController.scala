package net.petitviolet.ex.persistence.task.web

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import net.petitviolet.ex.persistence.task.actor.{ Register, AllTasks, GetAllTask, TaskPersistentActor }
import net.petitviolet.ex.persistence.task.model.TaskJsonSupport._
import net.petitviolet.ex.persistence.task.model.{ TaskTitle, Task }
import spray.json.{ RootJsonFormat, DefaultJsonProtocol }

import scala.concurrent.Future
import scala.language.postfixOps

trait WithTimeout {
  import scala.concurrent.duration._
  import akka.util.Timeout
  protected implicit val timeout = Timeout(1 second)
}

class TaskController extends JsonController
  with MixInGetTaskController with MixInRegisterTaskController
  with MixInAppContext {
  override val route: Route = getTaskController.route ~ registerTaskController.route
}

trait GetTaskController extends JsonController with WithTimeout {
  import appContext._
  private lazy val actor = system.actorOf(TaskPersistentActor.props)

  override val route: Route = (path("task" / "all") & get) {
    val tasks: Future[Seq[Task]] = (actor ? GetAllTask).mapTo[AllTasks].map { _.value }
    completeFuture(tasks)
  }

}

trait RegisterTaskController extends JsonController with WithTimeout {
  import appContext._
  import ResultJsonSupport._
  import RegisterTaskDTOJsonSupport._

  private lazy val actor = system.actorOf(TaskPersistentActor.props)

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

trait UsesGetTaskController {
  val getTaskController: GetTaskController
}

trait MixInGetTaskController {
  val getTaskController: GetTaskController = GetTaskControllerImpl
}

private object GetTaskControllerImpl extends GetTaskController with MixInAppContext

trait UsesRegisterTaskController {
  val registerTaskController: RegisterTaskController
}

trait MixInRegisterTaskController {
  val registerTaskController: RegisterTaskController = RegisterTaskControllerImpl
}

private object RegisterTaskControllerImpl extends RegisterTaskController with MixInAppContext