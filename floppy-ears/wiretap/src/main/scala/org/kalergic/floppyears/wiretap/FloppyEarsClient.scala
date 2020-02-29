package org.kalergic.floppyears.wiretap

import akka.dispatch.Dispatchers
import akka.http.scaladsl.model.{StatusCode => _}
import org.apache.avro.Schema
import org.json4s.JObject
import org.json4s.native.{JsonMethods, Serialization}
import play.api.Logger
import sttp.client._
import sttp.client.akkahttp.AkkaHttpBackend
import sttp.client.json4s._
import sttp.model.MediaType

import scala.concurrent.{ExecutionContext, Future}

trait FloppyEarsClient {
  def registerSchema(source: WiretapSource, schema: Schema): Future[Unit]
  def sendEvent(source: WiretapSource, event: JObject): Future[Unit]
}

object FloppyEarsClient {
  def apply(floppyEarsBaseUrl: String)(implicit dispatchers: Dispatchers): FloppyEarsClient =
    new FloppyEarsClientImpl(dispatchers, floppyEarsBaseUrl)

  private[wiretap] case class SchemaRegistration(source: WiretapSource, schema: JObject)
  private[wiretap] case class SchemaRegistrationResponse(source: WiretapSource, majorVersion: Int, minorVersion: Int)
  private[wiretap] case class FloppyEarsEvent(source: WiretapSource, event: JObject)
  private[wiretap] case class SendEventResponse(eventId: String)
}

class FloppyEarsClientImpl(dispatchers: Dispatchers, floppyEarsBaseUrl: String) extends FloppyEarsClient {

  import FloppyEarsClient._

  private[this] val logger = Logger(this.getClass)
  private[this] implicit val ec: ExecutionContext = dispatchers.lookup("floppy-ears")

  private[this] implicit val backend = AkkaHttpBackend()
  private[this] implicit val serialization: Serialization.type = org.json4s.native.Serialization

  // This code does not do any retries; in a real environment where a remote server controls
  // adjudicating whether a schema is compatible, you would want to implement some kind of retry strategy.
  // You would want to retry based on whether the error returned from the server is a client or server error.
  // Server errors and timeouts are likely to be retryable, while client errors may warrant some kind
  // of error handling or even app deployment failure, depending on the value your organization places on
  // schema compatibility.
  override def registerSchema(source: WiretapSource, schema: Schema): Future[Unit] = {
    val url = s"$floppyEarsBaseUrl/rest/v1/schemas"
    val schemaObj = JsonMethods.parse(schema.toString(false)).asInstanceOf[JObject]
    executeRequest(
      basicRequest
        .post(uri"$url").body(SchemaRegistration(source, schemaObj))
        .contentType(MediaType.ApplicationJson)
        .response(asJson[SchemaRegistrationResponse])) { regResponse =>
      logger.info(s"Successfully registered ${regResponse.source} with version ${regResponse.majorVersion}.${regResponse.minorVersion}")
      ()
    }
  }

  override def sendEvent(source: WiretapSource, event: JObject): Future[Unit] = {
    val url = s"$floppyEarsBaseUrl/rest/v1/events"
    executeRequest(
      basicRequest
        .post(uri"$url").body(FloppyEarsEvent(source, event))
        .contentType(MediaType.ApplicationJson)
        .response(asJson[SendEventResponse])) { _ =>
      logger.info(s"Successfully sent event for $source")
      ()
    }
  }

  private[this] def executeRequest[A, B](req: RequestT[Identity, Either[ResponseError[Exception], A], Nothing])
    (success: A => B): Future[B] = req.send().flatMap {
    case Response(body, statusCode, _, _, _) =>
      if(statusCode.isSuccess) {
        body match {
          case Left(error) =>
            logger.error(s"Error: $error\nInvalid response body:\n${error.body}")
            Future.failed(error)
          case Right(regResponse) => Future.successful(success(regResponse))
        }
      }
      else {
        body match {
          case Left(error) =>
            logger.error(s"Error: $error ${error.body}")
            Future.failed(error)
          case Right(_) =>
            logger.error(s"Error: Status code $statusCode")
            Future.failed(new Exception(s"Error: Status code $statusCode"))
        }
      }
  }
}
