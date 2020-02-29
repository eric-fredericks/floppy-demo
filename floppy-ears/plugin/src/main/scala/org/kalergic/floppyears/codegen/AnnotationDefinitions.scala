package org.kalergic.floppyears.codegen

import org.kalergic.floppyears.codegen.AnnotationParsing.ParsedAnnotations

private[codegen] object AnnotationDefinitions {

  sealed trait AnnotationDef {
    def name: String
    def params: Seq[AnnotationParamDef]

    def isIncluded(parsedAnnotations: ParsedAnnotations): Boolean = parsedAnnotations.parsed.contains(this)

    override def toString: String = name
  }

  case class AnnotationParamDef(annotationDef: AnnotationDef, paramName: String) {
    def getParsedValue(parsedAnnotations: ParsedAnnotations): Option[String] =
      for {
        params <- parsedAnnotations.parsed.get(annotationDef)
        value <- params.get(paramName)
      } yield {
        value
      }
  }

  // The annotation definitions:

  case object Wiretap extends AnnotationDef {
    override val name: String = "Wiretap"
    val actionName: AnnotationParamDef = AnnotationParamDef(this, "actionName")
    val majorVersion: AnnotationParamDef = AnnotationParamDef(this, "majorVersion")
    override val params: Seq[AnnotationParamDef] = Seq(actionName, majorVersion)
  }

  case object WiretapRequest extends AnnotationDef {
    override val name: String = "WiretapRequest"
    val requestBody: AnnotationParamDef = AnnotationParamDef(this, "requestBody")
    val parseJson: AnnotationParamDef = AnnotationParamDef(this, "parseJson")
    override val params: Seq[AnnotationParamDef] = Seq(requestBody, parseJson)
  }

  case object WiretapRequestTransform extends AnnotationDef {
    override val name: String = "WiretapRequestTransform"
    val transformedBody: AnnotationParamDef = AnnotationParamDef(this, "transformedBody")
    val transform: AnnotationParamDef = AnnotationParamDef(this, "transform")
    override val params: Seq[AnnotationParamDef] = Seq(transformedBody, transform)
  }

  case object WiretapResponse extends AnnotationDef {
    override val name: String = "WiretapResponse"
    val responseBody: AnnotationParamDef = AnnotationParamDef(this, "responseBody")
    override val params: Seq[AnnotationParamDef] = Seq(responseBody)
  }

  case object WiretapResponseTransform extends AnnotationDef {
    override val name: String = "WiretapResponseTransform"
    val transformedBody: AnnotationParamDef = AnnotationParamDef(this, "transformedBody")
    val transform: AnnotationParamDef = AnnotationParamDef(this, "transform")
    override val params: Seq[AnnotationParamDef] = Seq(transformedBody, transform)
  }

  case object WiretapIgnore extends AnnotationDef {
    override val name: String = "WiretapIgnore"
    override val params: Seq[AnnotationParamDef] = Seq.empty
  }

  case object WiretapConvert extends AnnotationDef {
    override val name: String = "WiretapConvert"
    val convert: AnnotationParamDef = AnnotationParamDef(this, "convert")
    override val params: Seq[AnnotationParamDef] = Seq(convert)
  }

  val supportedMethodAnnotations: Set[AnnotationDef] = Set(
    Wiretap,
    WiretapRequest,
    WiretapRequestTransform,
    WiretapResponse,
    WiretapResponseTransform)

  val supportedParameterAnnotations: Set[AnnotationDef] = Set(
    WiretapIgnore,
    WiretapConvert
  )

  val supportedAnnotationsByName: Map[String, AnnotationDef] =
    (supportedMethodAnnotations ++ supportedParameterAnnotations).toSeq.map {
      annotDef => annotDef.name -> annotDef
    }.toMap

}
