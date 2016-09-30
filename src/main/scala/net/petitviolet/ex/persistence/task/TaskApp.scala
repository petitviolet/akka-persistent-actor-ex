package net.petitviolet.ex.persistence.task

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.{ ActorMaterializer, ClosedShape }
import akka.stream.actor.ActorSubscriberMessage.{ OnComplete, OnNext }
import akka.stream.actor.{ OneByOneRequestStrategy, RequestStrategy, ActorSubscriber, ActorPublisher }
import akka.{ Done, NotUsed }
import com.typesafe.config.ConfigFactory
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._
import net.petitviolet.ex.persistence.task.web.MixInAppContext
import net.petitviolet.ex.persistence.task.web.controller.TaskController
import org.reactivestreams.Publisher

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Random

object TaskApp extends App {
  private class TaskAppActor extends Actor {
    private val taskActor: ActorRef = context.actorOf(TaskPersistentActor.props)

    private val r = new Random().nextInt(30)
    private val titles = r to r + 3 map { s => TaskTitle(s"todo_$s") }

    private def execute() = {
      //      taskActor ! Register(titles(0))
      //      taskActor ! Register(titles(1))
      //      taskActor ! Register(titles(2))
      //      taskActor ! Complete(titles(1))
      //  taskActor ! Complete(Task(titles(2)))
      //  taskActor ! Undo(Task(titles(1)))
      //      taskActor ! Archive(titles(0))
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

  implicit val system = ActorSystem("TaskList")
  val actor = system.actorOf(Props[TaskAppActor])

  actor ! Execute

  Thread.sleep(1000)
  system.terminate()
}

private object TaskWebApp extends App with MixInAppContext {
  import appContext._

  val config = ConfigFactory.load()
  //  KryoSerializerInitializer.run()

  val route = new TaskController().route
  val (host, port) = (config.getString("http.host"), config.getInt("http.port"))

  val binding = Http().bindAndHandle(
    Route.handlerFlow(route),
    host,
    port
  )

  println("start")

  val input = StdIn.readLine()

  binding.flatMap(_.unbind()).onComplete(_ => shutdown())
  println("end")
}

private object AkkaStreamPrac extends App {
  import akka.stream.scaladsl._
  implicit val system = ActorSystem("akka-stream-prac")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  case class Reply(value: String) extends AnyVal
  case object Finish

  class NiceActor extends ActorPublisher[Reply] {
    override def receive: Actor.Receive = {
      case s: String =>
        onNext(Reply(s"Nice: $s"))
      case i: Int =>
        onNext(Reply(s"Great: ${i * 100}"))
      case Finish =>
        onComplete()
    }
  }

  class PrintActor extends ActorSubscriber {
    override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

    override def receive: Actor.Receive = {
      case OnNext(any) => println(s"subscribed: $any")
      case OnComplete  => println(s"finish process!")
    }
  }

  val actorRef = system.actorOf(Props[NiceActor])

  val publisher: Publisher[Reply] = ActorPublisher(actorRef)

  val source: Source[Reply, NotUsed] = Source.fromPublisher(publisher)

  val flow: Flow[Reply, Reply, NotUsed] = Flow[Reply].map { r => r.copy(value = s"(Mapped: ${r.value})") }

  val accumulater: Flow[Reply, String, NotUsed] = Flow[Reply].fold("init") { (acc, rep) => s"$acc :: ${rep.value}" }

  val printActor = system.actorOf(Props[PrintActor])
  val sink: Sink[String, NotUsed] = Sink.fromSubscriber[String](ActorSubscriber[String](printActor))

  //  val graph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(source via flow via accumulater to sink)
  val graph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph {
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      source ~> flow ~> accumulater ~> sink
      ClosedShape
    }
  }

  graph.run

  // wait preparing graph
  Thread.sleep(100L)

  actorRef ! "hello!"

  actorRef ! 100

  actorRef ! "good"

  actorRef ! Finish

  StdIn.readLine()

  system.terminate()
}
