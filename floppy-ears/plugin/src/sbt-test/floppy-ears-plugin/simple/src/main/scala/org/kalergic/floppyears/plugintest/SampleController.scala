package org.kalergic.floppyears.plugintest

import org.kalergic.floppyears.plugintest.foo.{SomeExtraneousAnnotation, SomeId, TransformFunctions}
import org.kalergic.floppyears.wiretap.Annotations._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.EssentialAction

case class SampleInput(id: Int, x: String)
object SampleInput {
  implicit val sampleInputFormat: OFormat[SampleInput] = Json.format[SampleInput]
}

case class SampleData(id: Int)
object SampleData {
  implicit val sampleDataFormat: OFormat[SampleData] = Json.format[SampleData]
}

trait SampleController {

  @Wiretap("PostWithRequestAndResponse", majorVersion = 2)
  @WiretapRequest(requestBody = classOf[SampleInput])
  @WiretapResponse(classOf[SampleData])
  def postMethodWithRequestAndResponse(str: String): EssentialAction

  @Wiretap(actionName = "PostWithRequest", 3)
  @WiretapRequest(classOf[SampleInput], parseJson = false)
  def postMethodWithRequest(str1: String, @WiretapIgnore int1: Int, str2: Option[String]): EssentialAction

  @SomeExtraneousAnnotation
  @Wiretap(actionName = "GetWithResponse", majorVersion = 4)
  @WiretapResponse(classOf[SampleData])
  def getMethodWithResponse(
    @WiretapIgnore someId: SomeId,
    fooBar: Option[Boolean],
    @WiretapConvert(_.toBoolean) baz: Option[Boolean],
    bing: Boolean,
    strs1: Seq[String],
    @WiretapConvert(s => s) strs2: Seq[String],
    @WiretapConvert(identity) strs3: Seq[String],
    ints: Seq[Int],
    @WiretapConvert(_.toInt) ints1: Seq[Int],
    @WiretapConvert(SomeId(_)) ids: List[SomeId],
    @WiretapConvert(convert = someId => SomeId(someId)) dependentId: SomeId,
    @WiretapConvert(someId => SomeId(someId)) otherId: Option[SomeId],
    @WiretapConvert(convert = SomeId(_)) aThirdId: Option[SomeId],
    @WiretapConvert(SomeId(_)) aFourthId: Option[SomeId],
    id: Int,
    ln: Long,
    fl: Float,
    db: Double,
    str: String): EssentialAction

  @Wiretap(actionName = "GetWithNoParam", majorVersion = 1)
  @WiretapResponse(classOf[SampleData])
  def getMethodWithNoParam: EssentialAction

  def getMethodWithNoIntercept(id: Int): EssentialAction

  @Wiretap(actionName = "PostWithTransformedRequestAndResponse", majorVersion = 1)
  @WiretapRequest(classOf[SampleInput])
  @WiretapRequestTransform(transformedBody = classOf[SampleData], transform = TransformFunctions.sampleInputToSampleData)
  @WiretapResponse(classOf[SampleData])
  @WiretapResponseTransform(transformedBody = classOf[SampleData], transform = TransformFunctions.sampleDataIncrement)
  def postMethodWithTransformedRequestAndResponse: EssentialAction

  @Wiretap(actionName = "AnotherPostWithParsedBody", majorVersion = 1)
  @WiretapRequest(classOf[SampleInput], parseJson = false)
  @WiretapRequestTransform(transformedBody = classOf[SampleData], transform = TransformFunctions.sampleInputToSampleData)
  @WiretapResponse(classOf[SampleData])
  @WiretapResponseTransform(transformedBody = classOf[SampleData], transform = TransformFunctions.sampleDataIncrement)
  def anotherPostMethodWithParsedBodyAndTransformations: EssentialAction

}
