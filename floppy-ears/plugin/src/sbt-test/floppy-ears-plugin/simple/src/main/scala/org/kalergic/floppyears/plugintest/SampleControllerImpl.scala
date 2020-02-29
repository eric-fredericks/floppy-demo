package org.kalergic.floppyears.plugintest

import org.kalergic.floppyears.plugintest.foo.SomeId
import org.kalergic.floppyears.wiretap._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

// This is here to prove that all the static stuff is there - this stuff must compile!
// If it doesn't, the code generator is broken.
class SampleControllerImpl(controllerComponents: ControllerComponents)(
  implicit
  floppyEarsContext: FloppyEarsContext[MyAppRequest]
) extends AbstractController(controllerComponents) with SampleController {

  import FloppyEarsActionFunction._
  import SampleControllerFloppyEarsSupport._

  private[this] val postMethodWithRequestAndResponseContext: ActionContext[MyAppRequest] = postWithRequestAndResponseV2Context[MyAppRequest]
  private[this] val postMethodWithRequestAndResponseAction: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents).withFloppyEars(postMethodWithRequestAndResponseContext)
  override def postMethodWithRequestAndResponse(str: String): EssentialAction =
    postMethodWithRequestAndResponseAction.async { _ =>
        Future.successful(Ok(Json.toJson(new SampleData(15))))
    }

  private[this] val postMethodWithRequestContext: ActionContext[MyAppRequest] = postWithRequestV3Context[MyAppRequest]
  private[this] val postMethodWithRequestAction: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents).withFloppyEars(postMethodWithRequestContext)
  override def postMethodWithRequest(str1: String, int1: Int, str2: Option[String]): EssentialAction = {
    postMethodWithRequestAction.async(parse.json[SampleInput]) { _ =>
        Future.successful(Ok)
    }
  }

  private[this] val getMethodWithResponseContext: ActionContext[MyAppRequest] = getWithResponseV4Context[MyAppRequest]
  private[this] val getMethodWithResponseAction: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents).withFloppyEars(getMethodWithResponseContext)

  override def getMethodWithResponse(
    someId: SomeId,
    fooBar: Option[Boolean],
    baz: Option[Boolean],
    bing: Boolean,
    strs1: Seq[String],
    strs2: Seq[String],
    strs3: Seq[String],
    ints: Seq[Int],
    ints1: Seq[Int],
    ids: List[SomeId],
    dependentId: SomeId,
    otherId: Option[SomeId],
    aThirdId: Option[SomeId],
    aFourthId: Option[SomeId],
    id: Int,
    ln: Long,
    fl: Float,
    db: Double,
    str: String): EssentialAction =

    getMethodWithResponseAction.async { _ =>
      Future.successful(Ok(Json.toJson(SampleData(1))))
    }

  private[this] val getWithNoParamContext = getWithNoParamV1Context[MyAppRequest]
  private[this] val getMethodWithNoParamAction: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents).withFloppyEars(getWithNoParamContext)
  override def getMethodWithNoParam: EssentialAction =
    getMethodWithNoParamAction.async {
      Future.successful(Ok(Json.toJson(SampleData(861))))
    }

  private[this] val getMethodWithNoInterceptAction: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents)
  override def getMethodWithNoIntercept(id: Int): EssentialAction =
    getMethodWithNoInterceptAction.async { _ =>
      Future.successful(Ok(Json.toJson(SampleData(42))))
    }

  private[this] val postWithTransformationsContext = postWithTransformedRequestAndResponseV1Context[MyAppRequest]
  private[this] val postMethodWithTransformationsAction: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents).withFloppyEars(postWithTransformationsContext)
  override def postMethodWithTransformedRequestAndResponse: EssentialAction =
    postMethodWithTransformationsAction.async { _ =>
        Future.successful(Ok(Json.toJson(SampleData(431))))
    }

  private[this] val anotherPostWithParsedBodyContext = anotherPostWithParsedBodyV1Context[MyAppRequest]
  private[this] val anotherPostWithParsedBodyAction: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents).withFloppyEars(anotherPostWithParsedBodyContext)
  override def anotherPostMethodWithParsedBodyAndTransformations: EssentialAction = {
    anotherPostWithParsedBodyAction.async(parse.json[SampleInput]) { _ =>
      Future.successful(Ok(Json.toJson(SampleData(355))))
    }
  }
}
