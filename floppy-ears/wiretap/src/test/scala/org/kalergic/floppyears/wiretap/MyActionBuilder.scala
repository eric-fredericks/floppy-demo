package org.kalergic.floppyears.wiretap

import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

object MyActionBuilder {
  def apply(
      controllerComponents: ControllerComponents
  ): ActionBuilder[MyAppRequest, AnyContent] =
    new MyActionFunction(controllerComponents.executionContext)
      .andThen(new MyActionRefiner(controllerComponents.executionContext))
      .compose(controllerComponents.actionBuilder)
}

class MyActionFunction(override val executionContext: ExecutionContext)
    extends ActionFunction[Request, Request] {
  override def invokeBlock[A](
      request: Request[A],
      block: Request[A] => Future[Result]
  ): Future[Result] = block(request)
}

class MyActionRefiner(override val executionContext: ExecutionContext)
    extends ActionRefiner[Request, MyAppRequest] {
  override protected def refine[A](
      request: Request[A]
  ): Future[Either[Result, MyAppRequest[A]]] =
    Future.successful[Either[Result, MyAppRequest[A]]](
      Right(new MyAppRequest(request))
    )
}
