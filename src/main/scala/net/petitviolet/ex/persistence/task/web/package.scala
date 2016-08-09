package net.petitviolet.ex.persistence.task

import akka.actor.{ Actor, Props }

import scala.concurrent.Promise

package object web {

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
