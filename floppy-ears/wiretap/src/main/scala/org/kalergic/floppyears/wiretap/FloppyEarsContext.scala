package org.kalergic.floppyears.wiretap

import akka.dispatch.Dispatchers
import akka.stream.Materializer
import com.softwaremill.tagging.@@
import org.kalergic.floppyears.wiretap.ExecutionContextTags.PlayEC
import play.api.mvc.Request

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

trait FloppyEarsContext[R[_] <: Request[_]] {

  val sourceFor: ActionDef => Option[WiretapSource]

  val extractUserId: R[_] => Option[String]
  val extractSessionId: R[_] => Option[String]

  def dispatchers: Dispatchers
  def client: FloppyEarsClient

  implicit def materializer: Materializer

  def playEC: ExecutionContext @@ PlayEC
}
