package org.kalergic.floppyears.codegen

import java.nio.file.{Files, Path, Paths}

import play.routes.compiler._

object RoutesFileParserAdapter {

  /**
    * An adapter that delegates routes file parsing to the Play routes file parser and adapts the result.
    *
    * This adapter throws an exception if the routes file cannot be parsed to the satisfaction of the requirements of the Floppy Ears library.
    *
    * If successful, the parsed result is converted to a simplified representation of the defined route: a mapping of
    *
    *    "<package name>.<controller>.<method>" -> Map[String, String]
    *
    * ... where the inner map is a mapping of parameter names to their type expressions.
    *
    */
  def parse(routesFilePathStr: String): Map[String, Map[String, String]] = {

    val routesFilePath: Path = Paths.get(routesFilePathStr)
    require(
      Files.isRegularFile(routesFilePath),
      s"Play routes file is not a regular file: $routesFilePathStr"
    )

    RoutesFileParser.parse(routesFilePath.toFile) match {
      case Left(errors) =>
        throw new Exception(
          s"""Parse errors in routes file $routesFilePathStr:\n${errors
            .map(_.toString)
            .mkString("\n")}"""
        )
      case Right(rules) =>
        rules.flatMap {
          case Route(
              _,
              _,
              HandlerCall(
                packageName,
                controllerClassName,
                _,
                methodName,
                methodParameters
              ),
              _,
              _
              ) =>
            val fullyQualifiedMethodName =
              s"${packageName.getOrElse("")}.$controllerClassName.$methodName"
            val parameters: Seq[Parameter] = methodParameters.toSeq.flatten

            // We normalize to lower case during code generation.
            val normalizedParameterNames: Seq[String] =
              parameters.map(_.name.toLowerCase)

            // Ensure every controller param occurs exactly once without regard to case.
            normalizedParameterNames.foreach { normalizedName =>
              val occurrences =
                normalizedParameterNames.count(_ == normalizedName)
              if (occurrences > 1) {
                throw new Exception(
                  s"A parameter name must occur exactly once (without regard to case) in a routes file call definition. $normalizedName occurs $occurrences times for method $fullyQualifiedMethodName."
                )
              }
            }

            val handlerParams: Map[String, String] = parameters.map {
              case Parameter(name, typeName, _, _) =>
                name -> typeName
              case parameter @ _ =>
                // Play added something we were unaware of as of this writing.
                throw new Exception(
                  s"Got a parameter case that was not expected in routes file $routesFilePathStr: $parameter (This library is coded defensively and must be updated to handle this case)."
                )
            }.toMap
            Seq(fullyQualifiedMethodName -> handlerParams)

          case rule @ Include(_, _) =>
            println(s"Ignoring rule $rule in routes file: $routesFilePathStr")
            Seq.empty
          case rule @ _ =>
            // Play added something we were unaware of as of this writing.
            throw new Exception(
              s"Unknown rule case in routes file $routesFilePathStr: $rule (This library is coded defensively and must be updated to handle this case)."
            )
        }.toMap
    }
  }
}
