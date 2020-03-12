import Dependencies._
import sbt.Keys._

ThisBuild / organization := "org.kalergic.floppyears"
ThisBuild / name := "floppy-ears"
ThisBuild / version := "1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.10"

// enable publishing the jar produced by `test:package`
ThisBuild / publishArtifact in (Test, packageBin) := true

// enable publishing the test sources jar
ThisBuild / publishArtifact in (Test, packageSrc) := true

// disable publishing the root project
publishArtifact in `floppy-ears` := false

lazy val `floppy-ears` = (project in file("."))
  .aggregate(`floppy-ears-wiretap`, `floppy-ears-plugin`)
  .settings(
    assembly := { new File("") },
    crossScalaVersions := Nil,
    packageBin := { new File("") },
    packageSrc := { new File("") },
    publish := {},
    publishLocal := {}
  )

lazy val `floppy-ears-wiretap` = (project in file("wiretap"))
  .settings(
    scalacOptions += "-unchecked",
    crossScalaVersions := List("2.12.10", "2.13.1"),
    assembly := { new File("") },
    libraryDependencies ++= Seq(
      avro4s,
      json4s,
      play,
      sttpAkkaAsync,
      sttpJson4s,
      TestLibs.macwireMacros,
      TestLibs.macwireUtil,
      TestLibs.scalaTestPlusPlay
    )
  )

lazy val `floppy-ears-plugin` = (project in file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    Seq(
      name := "floppy-ears-plugin",
      publishMavenStyle := false,
      publishLocal := (publishLocal dependsOn (publishLocal in `floppy-ears-wiretap`)).value,
      sbtPlugin := true,
      libraryDependencies ++= Seq(
        playRoutesCompiler,
        scalameta
      )
    ) ++ scriptedOpts
  )

/*
 * All of this comes from http://eed3si9n.com/testing-sbt-plugins
 */
/**
  * The scripted sbt projects also need to know any sbt opt overrides. For example:
  * - if the .ivy2 location is in another place
  * - if logging options should be changed
  */
lazy val defaultSbtOpts =
  settingKey[Seq[String]]("The contents of the default_sbt_opts env var.")
lazy val javaOptsDebugger =
  settingKey[String]("Opens a debug port for scripted tests on 8000")
def scriptedOpts = Seq(
  defaultSbtOpts := {
    sys.env.get("default_sbt_opts").toSeq ++ sys.env.get("scripted_sbt_opts")
  },
  scriptedLaunchOpts := {
    scriptedLaunchOpts.value ++
      Seq(
        "-Xmx1024M",
        "-Dplugin.version=" + version.value,
        javaOptsDebugger.value
      ) ++
      defaultSbtOpts.value
  },
  javaOptsDebugger := "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
  scriptedBufferLog := false
)
