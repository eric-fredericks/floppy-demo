package org.kalergic.floppyears.codegen

import scala.collection.mutable
import scala.meta._
import scala.util.matching.Regex

private[codegen] case class BodyProcessing(
    rawBodyClass: String,
    bodyTransformation: Option[BodyTransformation]
) {
  def bodyClass: String =
    bodyTransformation
      .map {
        case (BodyTransformation(transformedClass, _)) =>
          transformedClass
      }
      .getOrElse(rawBodyClass)
}

private[codegen] case class BodyTransformation(
    transformedClass: String,
    transformFn: String
)

private[codegen] object AnnotationParsing {
  import AnnotationDefinitions._

  // Seems like overkill to parse the syntax tree here when a simple regex will do nicely.
  private[this] val ClassOf: Regex = """\AclassOf\[(.+)\]\z""".r

  private[this] def extractClass(expr: String): String = expr match {
    case ClassOf(clazz) => clazz
    case _              => throw new Exception(s"Invalid classOf expression: $expr")
  }

  // A helper for a parsed annotation with its parsed parameters.
  case class ParsedAnnotation(
      annotationDef: AnnotationDef,
      params: Map[String, String]
  )

  // A helper class to hold all the annotations and their param values for a syntax tree, with helper functions that inspect the
  // parsed annotations and pull out information needed by the code generator.
  case class ParsedAnnotations(context: String, mods: Seq[Mod]) {

    def annotationName(annot: Mod.Annot): String =
      annot
        .collect {
          case Type.Name(annotName) => annotName
        }
        .headOption
        .getOrElse {
          throw new Exception(s"Annotation $annot name not found: $context")
        }

    val parsed: Map[AnnotationDef, Map[String, String]] = mods
      .flatMap {
        case mod"@$annot " => Seq((annotationName(annot), annot))
        case _             => Seq.empty
      }
      .foldLeft[Map[AnnotationDef, Map[String, String]]](Map.empty) {
        case (map, (name, annot)) =>
          supportedAnnotationsByName.get(name).map { annotDef =>
            if (map.contains(annotDef)) {
              // Already saw this annotation!
              throw new Exception(
                s"Annotation with name $name is duplicated: $context"
              )
            }
            AnnotationParser(annot, annotDef).parse
          } match {
            case Some(ParsedAnnotation(defn, params)) =>
              map + (defn -> params)
            case None =>
              map
          }
      }

    def includesAnnotation(annotationDef: AnnotationDef): Boolean =
      annotationDef.isIncluded(this)

    def containsFloppyEarsMethodAnnotations: Boolean =
      supportedMethodAnnotations.exists(includesAnnotation)

    def containsFloppyEarsParameterAnnotations: Boolean =
      supportedParameterAnnotations.exists(includesAnnotation)

    val specifiesFloppyEarsInterceptable: Boolean = {
      val hasAnnotation = includesAnnotation(Wiretap)
      if (hasAnnotation && mods.exists {
            case mod"private[$_]   "   => true
            case mod"protected[$_]   " => true
            case mod"implicit   " | mod"final   " | mod"sealed   " |
                mod"override   " | mod"lazy   " | mod"valparam   " |
                mod"varparam   " | mod"case   " | mod"+   " | mod"-   " =>
              true
            //case mod"inline" => true
            // ^^^^ this isn't compiling, not sure why, so hence the next line
            case Mod.Inline() => true
            case _            => false
          }) {
        throw new Exception(
          s"Invalid modifier for annotation @$Wiretap (private | protected | implicit | final | sealed | override | case | lazy | valparam | varparam | inline"
        )
      }
      hasAnnotation
    }

    val specifiesIgnoredMethodParam: Boolean = includesAnnotation(WiretapIgnore)

    val requestBodyProcessing: Option[BodyProcessing] = {
      import WiretapRequest._
      import WiretapRequestTransform._
      bodyProcessing(requestBody, transformedBody, transform)
    }

    val responseBodyProcessing: Option[BodyProcessing] = {
      import WiretapResponse._
      import WiretapResponseTransform._
      bodyProcessing(responseBody, transformedBody, transform)
    }

    private[this] def bodyProcessing(
        bodyDefParam: AnnotationParamDef,
        transformBodyParam: AnnotationParamDef,
        transformFnParam: AnnotationParamDef
    ): Option[BodyProcessing] = {

      val maybeBodyClass = bodyDefParam.getParsedValue(this).map(extractClass)
      maybeBodyClass.map { bodyClass =>
        val maybeTransformedClass =
          transformBodyParam.getParsedValue(this).map(extractClass)
        val maybeTransformedFn = transformFnParam.getParsedValue(this)
        val maybeBodyTransformation =
          (maybeTransformedClass, maybeTransformedFn) match {
            case (Some(transformedClass), Some(transformedFn)) =>
              Some(BodyTransformation(transformedClass, transformedFn))
            case (None, None) =>
              None
            case _ =>
              throw new Exception(
                s"Missing annotation parameter for $bodyDefParam: both the transformed type and the function to transform the body to that type must be specified: $context."
              )
          }
        BodyProcessing(bodyClass, maybeBodyTransformation)
      }
    }

    val parseRequestJson: Option[Boolean] =
      WiretapRequest.parseJson.getParsedValue(this).map(_.toBoolean)

    val actionName: Option[String] = Wiretap.actionName.getParsedValue(this)

    val majorVersion: Option[Int] =
      Wiretap.majorVersion.getParsedValue(this).map(_.toInt)

    val convertFunction: Option[String] =
      WiretapConvert.convert.getParsedValue(this)
  }

  private[this] case class AnnotationParser(
      annot: Mod.Annot,
      annotationDef: AnnotationDef
  ) {

    // A custom Scalameta traverser that process the annotation syntax tree and converts it to a list of tuples
    // which are parameter key/value pairs.
    class AnnotationParamTraverser extends Traverser {

      val paramList: mutable.Builder[(String, String), List[(String, String)]] =
        List.newBuilder[(String, String)]

      override def apply(tree: Tree): Unit = tree match {
        case fun @ Term.Function(_, _) =>
          paramList += (("", fun.syntax))
        case applTpe @ Term.ApplyType(_, _) =>
          paramList += (("", applTpe.syntax))
        case appl @ Term.Apply(_, _) =>
          paramList += (("", appl.syntax))
        case select @ Term.Select(_, _) =>
          paramList += (("", select.syntax))
        case name @ Term.Name(_) =>
          paramList += (("", name.syntax))
        case Term.Assign(paramName, paramValue) =>
          paramList += (
            (
              paramName.syntax,
              paramValue.syntax.filterNot(_ == '"')
            )
          )
        case Lit.String(paramVal) =>
          paramList += (("", paramVal))
        case Lit.Int(paramVal) =>
          paramList += (("", paramVal.toString))
        case node =>
          super.apply(node)
      }

      apply(annot)
    }

    def parse: ParsedAnnotation = {

      // Normalize the parameters that were extracted from the source to include the parameter names.
      // This involves using the param name specified in the source when the annotation is declared,
      // or using positional information to determine the param name.
      def normalize(extracted: Seq[(String, String)]): Map[String, String] = {
        var names = annotationDef.params.map(_.paramName).toList
        for {
          (extractedName, value) <- extracted
        } yield {
          val name = extractedName match {
            case "" =>
              // Param name not specified in source, so we assume it is positional
              val name = names.head
              names = names.tail
              name
            case explicitName =>
              // Param name specified in source, so we remove the param name from the list
              names = names.filterNot(_ == explicitName)
              explicitName
          }
          (name, value)
        }
      }.toMap

      // Parse the parameters.
      val parsedParams = normalize(
        (new AnnotationParamTraverser).paramList.result
      )
      ParsedAnnotation(annotationDef, parsedParams)
    }
  }

  implicit class ModSeqOps(mods: Seq[Mod]) {
    def parseAnnotations(context: String): ParsedAnnotations =
      ParsedAnnotations(context, mods)
  }

}
