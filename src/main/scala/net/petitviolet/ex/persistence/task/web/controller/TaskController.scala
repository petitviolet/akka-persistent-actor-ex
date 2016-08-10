package net.petitviolet.ex.persistence.task.web.controller

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import net.petitviolet.ex.persistence.task.actor.{ AllTasks, GetAllTask, Register, TaskPersistentActor }
import net.petitviolet.ex.persistence.task.model.TaskJsonSupport._
import net.petitviolet.ex.persistence.task.model.TaskTitle
import net.petitviolet.ex.persistence.task.web.{ MixInAppContext, UsesAppContext }
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

import scala.concurrent.Promise
import scala.language.postfixOps

trait WithTimeout {
  import akka.util.Timeout

  import scala.concurrent.duration._
  protected implicit val timeout = Timeout(1 second)
}

trait UsesTaskActor {
  val taskActor: ActorRef
}

trait MixInTaskActor extends MixInAppContext {
  val taskActor: ActorRef = appContext.system.actorOf(TaskPersistentActor.props)
}

class TaskController extends JsonController
  with MixInGetTaskController with MixInRegisterTaskController
  with MixInAppContext {
  override val route: Route = getTaskController.route ~ registerTaskController.route
}

trait GetTaskController extends JsonController with UsesTaskActor with WithTimeout with UsesAppContext {
  import appContext._

  override val route: Route = (path("task" / "all") & get) {
    val promise = Promise[AllTasks]
    implicit val replyTo = system.actorOf(ResponseActor.props(promise))

    taskActor ! GetAllTask
    completeFuture(promise.future.map { _.value })
  }

}

trait RegisterTaskController extends JsonController with WithTimeout with UsesTaskActor with UsesAppContext {
  import RegisterTaskDTOJsonSupport._
  import ResultJsonSupport._

  override val route: Route = (path("task" / "new") & post) {
    entity(as[RegisterTaskDTO]) { dto =>
      taskActor ! Register(TaskTitle(dto.title))
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

private object GetTaskControllerImpl extends GetTaskController with MixInAppContext with MixInTaskActor

trait UsesRegisterTaskController {
  val registerTaskController: RegisterTaskController
}

trait MixInRegisterTaskController {
  val registerTaskController: RegisterTaskController = RegisterTaskControllerImpl
}

private object RegisterTaskControllerImpl extends RegisterTaskController with MixInAppContext with MixInTaskActor

