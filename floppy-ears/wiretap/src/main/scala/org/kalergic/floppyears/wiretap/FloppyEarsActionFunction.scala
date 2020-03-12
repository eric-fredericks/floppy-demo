package org.kalergic.floppyears.wiretap

import com.softwaremill.tagging._
import ExecutionContextTags.FloppyEarsEC
import play.api.Logger
import play.api.mvc._

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object ExecutionContextTags {
  sealed trait PlayEC
  sealed trait FloppyEarsEC
}

private[floppyears] class FloppyEarsActionFunction[R[_] <: Request[_]](
  actionContext: ActionContext[R],
  source: WiretapSource,
  playEC: ExecutionContext @@ ExecutionContextTags.PlayEC, // No implicits! We want to tell the compiler which one we want!
  tapEC: ExecutionContext @@ ExecutionContextTags.FloppyEarsEC // No implicits! We want to tell the compiler which one we want!
)(implicit floppyContext: FloppyEarsContext[R]) extends ActionFunction[R, R] {

  import actionContext._
  import floppyContext._

  // ActionFunction requires this - the EC to execute the request on.
  // No implicits! We want to tell the compiler which one we want!
  // That's the PlayEC. We'll use the TapEC to keep the wiretapping functionality decoupled from the Play execution context.
  protected def executionContext: ExecutionContext = playEC

  private[this] val logger = Logger(this.getClass)

  // This executes at startup and our requirements are to fail deployment if there is a schema registration error.
  // For the demo we will just do this blindly; in reality we would do some kind of retry in the face of server errors or timeouts.
  Await.result(
    awaitable = client.registerSchema(source, eventSchema),
    atMost = 5 seconds
  )

  override def invokeBlock[A](request: R[A], block: R[A] => Future[Result]): Future[Result] = {
    val resultF = block(request)
    resultF.andThen {
      case tryResult =>
        tap(request, tryResult)
    }(tapEC)
    // Return the future returned by the application.
    resultF
  }

  private[this] def tap[A](request: R[A], tryResult: Try[Result]): Unit = tryResult.map { result =>
    if (result.header.status >= 200 && result.header.status < 300) {
      result.body.consumeData.onComplete {
        case Success(bytes) =>
          val event = createEvent(request)(extractUserId)(extractSessionId)(bytes)
          client.sendEvent(source, event).onComplete {
            case Success(_) => logger.debug(s"Completed sending event for source=$source")
            case Failure(e) => logger.error("Error reporting data", e)
          }(tapEC)

        case Failure(e) => logger.error("Reporting skipped, materialization failed!", e)
      }(tapEC)
    } else {
      logger.debug("Reporting skipped because non-successful status")
    }
  }.recover {
    case NonFatal(_) =>
      logger.debug("Reporting skipped because future failed")
  }
}

object FloppyEarsActionFunction {

  private[this] val logger = Logger(this.getClass)

  implicit class ActionBuilderOps[R[_] <: Request[_], B](actionBuilder: ActionBuilder[R, B]) {
    def withFloppyEars[E](actionContext: ActionContext[R])(implicit floppyContext: FloppyEarsContext[R]): ActionBuilder[R, B] = {
      import actionContext._
      import floppyContext._

      sourceFor(actionDef).map { source =>
        val tapEC: ExecutionContext = dispatchers.lookup("floppy-ears")
        actionBuilder andThen new FloppyEarsActionFunction[R](
          actionContext,
          source,
          playEC = playEC,
          tapEC = tapEC.taggedWith[FloppyEarsEC]
        )
      }.getOrElse {
        // In case of a hotfix that breaks the schema, we need to be able to override the Floppy Ears interception behavior
        // to recover
        logger.info(s"Wiretap intercept bypass enabled for $actionDef. Events will not be forwarded to Floppy Ears.")
        actionBuilder
      }
    }
  }
}
