package net.petitviolet.ex.persistence.task.web

import akka.actor.{ Actor, Props }

import scala.concurrent.Promise

package object controller {

  object ResponseActor {
    def props[T](promise: Promise[T]) = Props(classOf[ResponseActor[T]], promise)
  }

  class ResponseActor[T](promise: Promise[T]) extends Actor {
    override def receive: Receive = {
      case result: T =>
        promise.success(result)
        context.stop(self)
    }
  }
}
