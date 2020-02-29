package org.kalergic.floppyears.wiretap

trait UrlBuilderSugar {

  implicit class QueryStringParamsSugar(url: String) {
    def withQueryParam(paramName: String, paramValue: String): String = {
      val base = if (url.contains("?")) s"$url&" else s"$url?"
      s"$base$paramName=$paramValue"
    }
  }
}
