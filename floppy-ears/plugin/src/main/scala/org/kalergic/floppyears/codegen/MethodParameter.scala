package org.kalergic.floppyears.codegen

import scala.meta.{Mod, Term}
import scala.util.matching.Regex

private[codegen] case class MethodParameter(
    parameterContext: String,
    param: Term.Param,
    mods: Seq[Mod]
) {
  import AnnotationDefinitions._
  import AnnotationParsing._
  import MethodParameter._

  val parsedAnnotations: ParsedAnnotations =
    mods.parseAnnotations(parameterContext)
  if (parsedAnnotations.containsFloppyEarsMethodAnnotations) {
    throw new Exception(
      s"Controller method parameter for $parameterContext contains invalid reference to a method annotation: ${supportedMethodAnnotations}"
    )
  }

  def isIgnored: Boolean = parsedAnnotations.specifiesIgnoredMethodParam

  def extractExpr(routeDefinedParams: Map[String, String]): String =
    param match {

      case Term.Param(_, name, Some(tpe), _) =>
        val paramName = name.syntax
        val routeDefinedTypeExpr = routeDefinedParams.getOrElse(
          paramName,
          throw new Exception(
            s"""No corresponding routes file parameter defined for parameter: $parameterContext. The parameter name in the controller trait definition must exactly match the parameter name used in the routes file. If you're adding Floppy Ears instrumentation to an existing endpoint, please change the method declaration in the controller trait to match the parameter name used in the url path or query string."""
          )
        )

        val tpeExpr = tpe.syntax
        if (tpeExpr != routeDefinedTypeExpr) {
          throw new Exception(
            s"""Type expression $tpeExpr for parameter $parameterContext in trait defining controller must match the type expression used in the routes file: "$routeDefinedTypeExpr""""
          )
        }

        // Determine developer's intention: was a function of type `String => T` declared in a `@WiretapConvert` annotation?
        val convertExpr =
          if (parsedAnnotations.includesAnnotation(WiretapConvert)) {
            // Developer-specified parameter conversion expression: String => T
            val fn = parsedAnnotations.convertFunction
            if (fn.isEmpty) {
              throw new Exception(
                s"No conversion function defined for @$WiretapConvert annotation for parameter: $parameterContext"
              )
            } else {
              s"${fn.get}"
            }
          } else {
            // Use default ".to<Type>" (e.g. .toInt, .toDouble, etc.)
            tpeExpr match {
              case Wrapper(_, thing) =>
                s"_.to$thing"
              case thing =>
                s"_.to$thing"
            }
          }

        // This expression gives us a Seq[String].
        val getParamExpr =
          s"""requestParams.get("${name.syntax.toLowerCase}").toSeq.flatten"""

        // After mapping to the correct type, we will need to convert Seq[T] to whatever the method signature has declared.
        val (toWrapperExpr, getExpr) = tpeExpr match {
          case Wrapper("Option", _) =>
            (".headOption", "")
          case Wrapper("Seq", _) =>
            ("", "")
          case Wrapper("List", _) =>
            (".toList", "")
          case _ =>
            (".headOption", getExprFor(convertExpr))
        }

        s"""$name=$getParamExpr$toWrapperExpr.map($convertExpr)$getExpr"""

      case somethingElse @ _ =>
        throw new Exception(
          s"Unable to determine extractExpr: ${somethingElse.syntax} for $parameterContext"
        )
    }

  private[this] def getExprFor(convertExpr: String): String =
    convertExpr match {
      case "_.toBoolean"                                     => ".getOrElse(false)"
      case "_.toInt" | "_.toLong" | "_.toShort" | "_.toByte" => ".getOrElse(0)"
      case "_.toDouble"                                      => ".getOrElse(0.0)"
      case "_.toFloat"                                       => ".getOrElse(0.0f)"
      case "_.toString"                                      => """.getOrElse("")"""
      case _                                                 => ".get"
    }
}

private[codegen] case object MethodParameter {
  val Wrapper: Regex = "(.+)\\[(.+)\\]".r
}
