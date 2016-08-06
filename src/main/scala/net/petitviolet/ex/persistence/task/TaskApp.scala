package net.petitviolet.ex.persistence.task

import akka.actor.ActorSystem
import net.petitviolet.ex.persistence.task.actor._
import net.petitviolet.ex.persistence.task.model._

import scala.util.Random

object TaskApp extends App {
  implicit val system = ActorSystem("TaskList")
  val actor = system.actorOf(TaskPersistentActor.props)

  actor ! Print

  val r = new Random().nextInt(30)
  val titles = r to r + 3 map { s => TaskTitle(s"todo_$s") }
  actor ! Register(titles(0))
  actor ! Register(titles(1))
  actor ! Register(titles(2))
  actor ! Complete(Task(titles(1)))
//  actor ! Complete(Task(titles(2)))
//  actor ! Undo(Task(titles(1)))
  actor ! Archive(Task(titles(0)))

  actor ! Print
  actor ! Snapshot

  Thread.sleep(1000)
  system.terminate()
}
