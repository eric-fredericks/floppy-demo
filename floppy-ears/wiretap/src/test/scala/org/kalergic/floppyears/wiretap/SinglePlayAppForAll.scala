package org.kalergic.floppyears.wiretap

import akka.stream.Materializer
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Logger, Mode}

object SinglePlayAppForAll {

  private[this] val logger = Logger(this.getClass)

  logger.warn("Starting Play App. Should show only once.")

  val path: java.io.File = new java.io.File(".")
  val classloader: ClassLoader = getClass.getClassLoader
  val routes: PartialFunction[Any, Nothing] = PartialFunction.empty
  val fakeApp: Application = new GuiceApplicationBuilder()
    .in(Environment(path, classloader, Mode.Test))
    .routes(routes)
    .build

  play.api.Play.start(fakeApp)
}

trait SinglePlayAppForAll {
  implicit val app: Application = SinglePlayAppForAll.fakeApp
  implicit def mat: Materializer = app.materializer
  def shutPlayAppDown(): Unit = play.api.Play.stop(app)
}
