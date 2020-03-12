package org.kalergic.floppyears.plugin

import org.kalergic.floppyears.codegen.{
  RoutesFileParserAdapter,
  FloppyEarsSupportGenerator
}
import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.file.{Path, Paths}

import sbt.Keys._
import sbt._
import sbt.plugins._

import scala.util.control.NonFatal

object FloppyEarsPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  val floppyEarsSourceSubdirName = "floppyears"

  /**
    * Invoke the generator.
    *
    * @param inputs The source files to read.
    * @param outDirPath The place to write generated sources to.
    * @param s sbt task streams (for logging).
    * @return
    */
  private[this] def generateFloppyEarsSupport(
      routesFilePath: String,
      inputs: Seq[File],
      outDirPath: Path,
      s: TaskStreams
  ): Set[File] = {
    s.log.info(
      "Floppy Ears Plugin: Hello! Generating Floppy Ears support code..."
    )

    var ps: Option[PrintStream] = None
    try {
      val routes = RoutesFileParserAdapter.parse(routesFilePath)
      FloppyEarsSupportGenerator.generateInterceptors(
        routes,
        outDirPath,
        inputs.map(_.absolutePath)
      )
      s.log.info("Floppy Ears Plugin: Code generation complete. Goodbye.")
    } catch {
      case NonFatal(e) =>
        try {
          // I want to print the stack trace but the TaskStreams.log thing doesn't have a "normal" .error method - no parameter for
          // a Throwable...
          //     final def error(message: => String): Unit = log(Level.Error, message)
          // ... that's all you get.
          val os = new ByteArrayOutputStream
          ps = Some(new PrintStream(os))
          ps.foreach(e.printStackTrace)
          val stackTrace = os.toString
          s.log.error(s"Code generation failed\n$stackTrace")
        } finally {
          ps.foreach(_.close())
        }
    }

    // Must return the files generated!
    getGeneratedFileList(outDirPath)
  }

  def getGeneratedFileList(path: Path): Set[File] = {
    path.toFile
      .listFiles(new java.io.FileFilter {
        override def accept(f: File): Boolean = {
          f.getName.endsWith(""".scala""")
        }
      })
      .toSet
  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    sourceGenerators in Compile += Def.task {

      val sourceManagedFolder: File = (sourceManaged in Compile).value

      // I shamelessly stole the caching approach from
      // https://github.com/sbt/sbt-avro/blob/master/src/main/scala/sbtavro/SbtAvro.scala
      val out = streams.value
      val outDirPath: Path = getFloppyEarsOutdirPath(sourceManagedFolder)

      val baseDirPath = baseDirectory.value.getAbsolutePath
      val separator = java.io.File.separator
      val routesFilePath = s"$baseDirPath${separator}conf${separator}routes"
      val cachedGenFn: Set[File] => Set[File] =
        getCachedGenFunction(routesFilePath, out, outDirPath)
      val inputs: Seq[File] = (unmanagedSources in Compile).value
      cachedGenFn(inputs.toSet).toSeq

    }.taskValue
  )

  def getFloppyEarsOutdirPath(sourceManagedFolder: sbt.File): Path = {
    val outDirPathStr = {
      val folder = new File(sourceManagedFolder, floppyEarsSourceSubdirName)
      if (folder.exists) {
        require(folder.isDirectory, s"file :[$folder] is not a directory")
      } else {
        folder.mkdirs()
      }
      folder.getPath
    }
    Paths.get(outDirPathStr)
  }

  def getCachedGenFunction(
      routesFilePath: String,
      out: TaskStreams,
      outDirPath: Path
  ): Set[File] => Set[File] = {
    FileFunction.cached(
      out.cacheDirectory / floppyEarsSourceSubdirName,
      inStyle = FilesInfo.lastModified,
      outStyle = FilesInfo.exists
    ) { fileSet =>
      generateFloppyEarsSupport(routesFilePath, fileSet.toSeq, outDirPath, out)
    }
  }
}
