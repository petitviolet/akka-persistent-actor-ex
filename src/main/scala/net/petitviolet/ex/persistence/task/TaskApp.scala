package net.petitviolet.ex.persistence.task

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._
import net.petitviolet.ex.persistence.task.web.MixInAppContext
import net.petitviolet.ex.persistence.task.web.controller.TaskController

import scala.io.StdIn
import scala.util.Random

object TaskApp extends App {
  implicit val system = ActorSystem("TaskList")
  val actor = system.actorOf(Props[TaskAppActor])

  actor ! Execute

  Thread.sleep(1000)
  system.terminate()
}

private class TaskAppActor extends Actor {
  private val taskActor: ActorRef = context.actorOf(TaskPersistentActor.props)

  private val r = new Random().nextInt(30)
  private val titles = r to r + 3 map { s => TaskTitle(s"todo_$s") }

  private def execute() = {
    taskActor ! Register(titles(0))
    taskActor ! Register(titles(1))
    taskActor ! Register(titles(2))
    taskActor ! Complete(Task(titles(1)))
    //  taskActor ! Complete(Task(titles(2)))
    //  taskActor ! Undo(Task(titles(1)))
    taskActor ! Archive(Task(titles(0)))
    taskActor ! GetNotCompleted
    taskActor ! GetAllTask

    //    taskActor ! Print
    //    taskActor ! Snapshot
  }

  override def receive: Receive = {
    case Execute                  => execute()
    case AllTasks(value)          => println(s"\nAll TaskList: $value\n")
    case NotCompletedTasks(value) => println(s"\nNotCompleted TaskList: $value\n")
  }
}

private case object Execute

private object TaskWebApp extends App with MixInAppContext {
  import appContext._

  val config = ConfigFactory.load()

  val route = new TaskController().route
  val (host, port) = (config.getString("http.host"), config.getInt("http.port"))

  val binding = Http().bindAndHandle(
    Route.handlerFlow(route),
    host,
    port
  )

  println("start")

  StdIn.readLine()

  binding.flatMap(_.unbind())
  println("end")

  appContext.shutdown()
}
