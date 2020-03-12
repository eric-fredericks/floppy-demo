package org.kalergic.floppyears.wiretap

import org.apache.avro.Schema
import org.json4s.JObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FakeFloppyEarsClient {
  case class SendEvent(source: WiretapSource, event: JObject, thread: Thread)
  case class SchemaRegistration(source: WiretapSource, schema: Schema)
}

class FakeFloppyEarsClient extends FloppyEarsClient {

  import FakeFloppyEarsClient._

  @volatile var _failRegistration: Boolean = false
  @volatile var _sleepOnRegistrationMillis: Long = 0

  @volatile var _failSendEvent: Boolean = false
  @volatile var _failureException: Exception = new Exception(
    "Intentionally thrown by test"
  )

  @volatile var _schemaRegistrations: List[SchemaRegistration] = List.empty
  @volatile var _events: List[SendEvent] = List.empty

  override def registerSchema(
      source: WiretapSource,
      schema: Schema
  ): Future[Unit] =
    if (_failRegistration) {
      Future.failed(_failureException)
    } else {
      Future {
        if (_sleepOnRegistrationMillis > 0) {
          // Go async -- user is testing a timeout
          Thread.sleep(_sleepOnRegistrationMillis)
        }
        _schemaRegistrations +:= SchemaRegistration(source, schema)
      }
    }

  def sendEvent(source: WiretapSource, event: JObject): Future[Unit] =
    if (_failSendEvent) {
      Future.failed(_failureException)
    } else {
      Future.successful {
        _events +:= SendEvent(source, event, Thread.currentThread)
      }
    }
}
