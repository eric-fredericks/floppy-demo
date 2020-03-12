package org.kalergic.floppyears.wiretap

import akka.dispatch.Dispatchers
import akka.stream.Materializer
import com.softwaremill.tagging._
import org.kalergic.floppyears.wiretap.ExecutionContextTags.PlayEC
import play.api.Application

import scala.concurrent.ExecutionContext

class MyFloppyEarsContext(implicit app: Application)
    extends FloppyEarsContext[MyAppRequest] {

  override val sourceFor: ActionDef => Option[WiretapSource] = action =>
    Option(
      WiretapSource(
        environment = "unit-test",
        application = "bar",
        actionName = action.name,
        majorVersion = action.majorVersion
      )
    )

  override val extractUserId: MyAppRequest[_] => Option[String] = _ =>
    Some("dummy-user-id")

  override val extractSessionId: MyAppRequest[_] => Option[String] = _ =>
    Some("dummy-session-id")

  override implicit def materializer: Materializer = app.materializer
  override implicit def dispatchers: Dispatchers = app.actorSystem.dispatchers

  override val client: FakeFloppyEarsClient = new FakeFloppyEarsClient

  val playEC: ExecutionContext @@ PlayEC =
    app.actorSystem.dispatcher.taggedWith[PlayEC]
}
