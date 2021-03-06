package org.kalergic.floppyears.wiretap

import akka.util.ByteString
import org.apache.avro.Schema
import org.json4s.JObject
import play.api.mvc.Request

import scala.language.postfixOps

case class ActionDef(name: String, majorVersion: Int) {
  require(name.nonEmpty, "The CActionDef name must be non-empty.")
  require(majorVersion >= 0, "The major version must be greater than 0")
}

case class ActionContext[R[_] <: Request[_]](
    actionDef: ActionDef,
    eventSchema: Schema,
    createEvent: R[_] => // The request
    (R[_] => Option[String]) => // The user id extraction function
    (R[_] => Option[String]) => // The session id extraction function
    ByteString => // The byte array containing the result produced by the action
    JObject // The resulting event object, as a Json object, to be sent to the data science backend
)
