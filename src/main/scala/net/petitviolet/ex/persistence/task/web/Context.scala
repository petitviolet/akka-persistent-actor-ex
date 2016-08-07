package net.petitviolet.ex.persistence.task.web

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

trait Context {
  implicit val system: ActorSystem
  implicit val executor: ExecutionContextExecutor
  implicit val materializer: ActorMaterializer

  private[application] def shutdown() = system.terminate()
}

trait UsesContext {
  val context: Context
}

trait MixInContext {
  implicit val context: Context = ContextImpl
}

object ContextImpl extends Context {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
}



