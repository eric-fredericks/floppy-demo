package org.kalergic.recipebox.module

import akka.actor.ActorSystem
import akka.dispatch.Dispatchers
import akka.stream.Materializer
import com.softwaremill.tagging._
import org.kalergic.floppyears.wiretap.{ActionDef, ExecutionContextTags, FloppyEarsClient, FloppyEarsContext, WiretapSource}
import play.api.mvc.Request

import scala.concurrent.ExecutionContext

class MyFloppyEarsContext(
    val playActorSystem: ActorSystem,
    override val client: FloppyEarsClient,
    override val materializer: Materializer
  )
  extends FloppyEarsContext[Request] {

  override val sourceFor: ActionDef => Option[WiretapSource] =
    actionDef => Some(WiretapSource("dev", "recipe-box", actionDef.name, actionDef.majorVersion))

  // In a real app you would use something real here
  override val extractUserId: Request[_] => Option[String] = _ => None

  // In a real app you would use something real here
  override val extractSessionId: Request[_] => Option[String] = _ => None

  override def dispatchers: Dispatchers = playActorSystem.dispatchers

  override def playEC: ExecutionContext @@ ExecutionContextTags.PlayEC =
    playActorSystem.dispatcher.taggedWith[ExecutionContextTags.PlayEC]
}
