package net.petitviolet.ex.persistence.task.web

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

trait AppContext {
  implicit val system: ActorSystem
  implicit val executor: ExecutionContext
  implicit val materializer: ActorMaterializer

  def shutdown() = system.terminate()
}

trait UsesAppContext {
  val appContext: AppContext
}

trait MixInAppContext {
  implicit val appContext: AppContext = ContextImpl
}

private object ContextImpl extends AppContext {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
}

