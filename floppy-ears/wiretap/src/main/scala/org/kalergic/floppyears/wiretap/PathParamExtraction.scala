package org.kalergic.floppyears.wiretap

import java.util.regex.Pattern

import play.api.Logger
import play.api.mvc.Request
import play.api.routing.Router

import scala.collection.concurrent
import scala.util.matching.Regex.Groups

// We're composing action functions, and our WiretapActionFunction does not have access to the URL path parameters because Play does not
// expose them in the request (unlike the query string parameters). The type class defined in this object handles the extraction, which is
// used in the code produced by the generator.
//
// We are essentially repeating the URL path param extraction that Play did; because we have chosen to intercept data with a composed
// action function and we necessarily lose the type information at that level. The code produced by the generator will handle converting
// the String values yielded by our type class to the proper type when constructing Wiretap events.
object PathParamExtraction {

  private[this] val logger = Logger(this.getClass)

  // Look for these patterns and extract these groups from the route pattern which Play adds
  // to a request's tags.
  private[this] val PathParamKeyPattern = """\$(.+?)\<\[\^\/\]\+\>"""

  // Cache the route pattern metadata so you don't have to compute it over and over.
  // This data includes the path variable names and the search pattern to use on a
  // request url to dynamically extract the values for a particular request.
  private[this] case class RoutePatternMeta(
      pathVarNames: Seq[String],
      searchPattern: String
  )
  private[this] val routeCache =
    new concurrent.TrieMap[String, RoutePatternMeta]

  // Type class to extract the path params from the request.
  implicit class PathParamsExtractor[R[_] <: Request[_]](request: R[_]) {

    def pathParams: Map[String, Seq[String]] = {
      getRoutePatternMeta(request)
        .map { meta =>
          val values: Seq[String] = extractPathParamValues(request, meta)

          // We will merge the pathParams with the queryParams provided by Play in the generated code.
          // The type must be Map[String,Seq[String]].
          meta.pathVarNames.zip(values).toMap.map {
            case (k, v) => (k, Seq(v))
          }

        }
        .getOrElse(Map.empty)
    }

    private[this] def extractPathParamValues(
        request: R[_],
        meta: RoutePatternMeta
    ): Seq[String] = {
      // For some reason the scala API isn't working as I'd expect. The code below using the Java Regex API is working.
      // I'm leaving this commented out in case we ever want to revisit this.
      // meta.searchPattern.r.findAllIn(request.path).toSeq

      val pat = Pattern.compile(meta.searchPattern)
      val mat = pat.matcher(request.path)
      if (mat.find) {
        for (i <- 1 to mat.groupCount) yield mat.group(i)
      } else {
        logger.warn(
          s"Param values not found for request with path ${request.path}"
        )
        Seq.empty
      }
    }
  }

  private[PathParamExtraction] val extractPathParamKeys
      : String => RoutePatternMeta = { routePattern =>
    val keys: Seq[String] =
      PathParamKeyPattern.r.findAllMatchIn(routePattern).toList.flatMap {
        case Groups(extractedGroups) => Some(extractedGroups)
        case _                       => None
      }
    // Each path param will need to be extracted from the correct position in the route pattern.
    val searchPattern =
      PathParamKeyPattern.r.replaceAllIn(routePattern, "([^/]+)")
    RoutePatternMeta(keys, searchPattern)
  }

  private[PathParamExtraction] def getRoutePatternMeta[R[_] <: Request[_]](
      request: R[_]
  ): Option[RoutePatternMeta] =
    // Play tags every request with the route pattern it used to determine where to dispatch the request.
    request.attrs
      .get(Router.Attrs.HandlerDef)
      .map { handlerDef =>
        handlerDef.path
      }
      .map { routePattern =>
        routeCache.getOrElseUpdate(
          routePattern,
          extractPathParamKeys(routePattern)
        )
      }
}
