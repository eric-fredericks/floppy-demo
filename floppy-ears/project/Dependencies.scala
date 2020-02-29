import sbt._

object Dependencies {

  private val scalametaVersion = "4.3.0"
  private val playVersion = "2.8.1"
  private val avro4sVersion = "3.0.8"
  private val sttpVersion = "2.0.3"
  private val json4sVersion = "3.6.7"

  val avro4s = "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion

  val play = "com.typesafe.play" %% "play" % playVersion exclude("com.typesafe.play", "play-json_2.13")

  val playRoutesCompiler = "com.typesafe.play" %% "routes-compiler" % playVersion
  val scalameta = "org.scalameta" %% "scalameta" % scalametaVersion
  val sttpAkkaAsync = "com.softwaremill.sttp.client" %% "akka-http-backend" % sttpVersion
  val sttpJson4s = "com.softwaremill.sttp.client" %% "json4s" % sttpVersion
  val json4s = "org.json4s" %% "json4s-native" % json4sVersion

  object TestLibs {

    private val macwireVersion = "2.3.3"
    private val scalaTestPlusPlayVersion = "5.0.0"

    val macwireMacros = "com.softwaremill.macwire" %% "macros" % macwireVersion % Test
    val macwireUtil = "com.softwaremill.macwire" %% "util" % macwireVersion
    val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % Test
  }
}
