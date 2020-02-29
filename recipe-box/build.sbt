organization := "org.kalergic.goodfood"
name := """recipe-box"""
version := "1.0-SNAPSHOT"

scalaVersion := "2.13.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala, FloppyEarsPlugin)
pipelineStages := Seq(digest)

libraryDependencies ++= Seq(
  jdbc,
  caffeine,
  ws,
  "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided",
  "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.8",
  "org.kalergic.floppyears" %% "floppy-ears-wiretap" % "1.0-SNAPSHOT"
)
