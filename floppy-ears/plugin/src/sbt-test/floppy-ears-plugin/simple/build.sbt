lazy val root = (project in file("."))
  .enablePlugins(FloppyEarsPlugin)
  .settings(
    version := "0.1",
    scalaVersion := "2.12.10",
    libraryDependencies ++= Seq(
      "org.kalergic.floppyears" %% "floppy-ears-wiretap" % "1.0-SNAPSHOT" % "compile->test;test->test",
      "com.typesafe.play" %% "play" % "2.8.1",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    )
  )
