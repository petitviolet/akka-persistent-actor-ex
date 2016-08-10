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

class TaskController extends JsonController
  with MixInAppContext {

  val taskActor: ActorRef = appContext.system.actorOf(TaskPersistentActor.props)

  private class GetTaskControllerImpl(actorRef: ActorRef) extends GetTaskController(actorRef) with MixInAppContext
  private class RegisterTaskControllerImpl(actorRef: ActorRef) extends RegisterTaskController(actorRef) with MixInAppContext

  override val route: Route =
    new GetTaskControllerImpl(taskActor).route ~
      new RegisterTaskControllerImpl(taskActor).route
}

abstract class GetTaskController(taskActor: ActorRef) extends JsonController with WithTimeout with UsesAppContext {
  import appContext._

  override val route: Route = (path("task" / "all") & get) {
    val promise = Promise[AllTasks]
    implicit val replyTo = system.actorOf(ResponseActor.props(promise))

    taskActor ! GetAllTask
    completeFuture(promise.future.map { _.value })
  }

}

abstract class RegisterTaskController(taskActor: ActorRef) extends JsonController with WithTimeout with UsesAppContext {
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

