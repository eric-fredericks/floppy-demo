package org.kalergic.floppyears.wiretap

import play.api.libs.json._

case class SomePayload(x: Int)

object SomePayload {
  implicit val somePayloadFormat: OFormat[SomePayload] = Json.format[SomePayload]
}
