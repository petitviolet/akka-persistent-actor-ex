package net.petitviolet.ex.persistence.task.web

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

trait Controller {
  //  implicit val context: Context
  //  protected implicit val ec: ExecutionContext = context.executor
  val route: Route
}

trait JsonController extends Controller with SprayJsonSupport with UsesAppContext {
  protected def completeFuture[A](resultFuture: Future[A])(implicit marshaller: ToResponseMarshaller[A]) =
    onSuccess(resultFuture) { result =>
      complete(result)
    }
}

