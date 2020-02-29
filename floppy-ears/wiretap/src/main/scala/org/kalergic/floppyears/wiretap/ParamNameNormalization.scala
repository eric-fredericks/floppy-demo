package org.kalergic.floppyears.wiretap

object ParamNameNormalization {

  implicit class ParamNameNormalizer(params: Map[String, Seq[String]]) {
    def normalizeParamNames: Map[String, Seq[String]] = {
      params.map {
        case (paramName, paramValues) => paramName.toLowerCase -> paramValues
      }
    }
  }
}
