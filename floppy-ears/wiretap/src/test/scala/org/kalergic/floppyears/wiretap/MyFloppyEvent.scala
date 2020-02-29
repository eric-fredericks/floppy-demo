package org.kalergic.floppyears.wiretap

import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.Schema

case class MyFloppyEvent(_userId: Option[String], _sessionId: Option[String], a: Int, b: String, _requestBody: SomePayload, _responseBody: Option[SomePayload])

object MyFloppyEvent {
  implicit val schema: Schema = AvroSchema[MyFloppyEvent]
}
