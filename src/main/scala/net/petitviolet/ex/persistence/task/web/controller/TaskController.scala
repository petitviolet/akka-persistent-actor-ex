package net.petitviolet.ex.persistence.task.web.controller

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ StandardRoute, Route }
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model.TaskJsonSupport._
import net.petitviolet.ex.persistence.task.model.{ TaskState, TaskId, Task, TaskTitle }
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

  // I wonder what is the best way to pass actor to child controllers...?
  val taskActor: ActorRef = appContext.system.actorOf(TaskPersistentActor.props)

  private class GetTaskControllerImpl(actorRef: ActorRef) extends GetTaskController(actorRef) with MixInAppContext
  private class CommandTaskControllerImpl(actorRef: ActorRef) extends CommandTaskController(actorRef) with MixInAppContext

  override val route: Route =
    new GetTaskControllerImpl(taskActor).route ~
      new CommandTaskControllerImpl(taskActor).route
}

abstract class GetTaskController(taskActor: ActorRef) extends JsonController with WithTimeout with UsesAppContext {
  import appContext._

  override val route: Route = (path("task" / "all") & get) {
    parameter('status.as[Int]?).as { statusOpt: Option[Int] =>
      statusOpt map TaskState.from
    } { taskStateOpt: Option[TaskState] =>
      val promise = Promise[TaskList]
      implicit val replyTo = system.actorOf(ResponseActor.props(promise))
      val event: QueryEvent = taskStateOpt map QueryEvent.byState getOrElse GetAllTask

      taskActor ! event
      completeFuture(promise.future.map {
        _.value
      })
    }
  }

}

abstract class CommandTaskController(taskActor: ActorRef) extends JsonController with WithTimeout with UsesAppContext {
  import RegisterTaskDTOJsonSupport._
  import ResultJsonSupport._

  // should separate each Controller...
  private val commandMap: Map[String, (TaskTitle => CommandEvent)] = Map(
    "new" -> Register.apply,
    "done" -> Complete.apply,
    "undo" -> Undo.apply,
    "delete" -> Archive.apply
  )

  private val _routes = commandMap map {
    case (_path, constructor) =>
      entity(as[TaskTitleDTO]) { dto =>
        (path(_path) & post) { // should use RESTful HTTP-method...
          val event: CommandEvent = constructor(TaskTitle(dto.title))
          taskActor ! event
          complete(Result("ok"))
        }
      }
  }

  override val route: Route = pathPrefix("task") {
    _routes.reduce { _ ~ _ }
  }

}

private case class TaskTitleDTO(title: String)
private case class Result(result: String)

private object RegisterTaskDTOJsonSupport extends DefaultJsonProtocol {
  implicit val registerTaskFormat: RootJsonFormat[TaskTitleDTO] = jsonFormat1(TaskTitleDTO)
}

private object ResultJsonSupport extends DefaultJsonProtocol {
  implicit val resultFormat: RootJsonFormat[Result] = jsonFormat1(Result)
}

