package org.kalergic.floppyears.wiretap

import com.softwaremill.macwire._
import org.json4s._
import org.kalergic.floppyears.wiretap.FakeFloppyEarsClient.{SchemaRegistration, SendEvent}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future

object FloppyEarsActionFunctionSpec extends SinglePlayAppForAll {

  class FloppyEarsActionFunctionFixture extends BaseController {
    import MyFloppyEvent._
    import SomePayload._

    implicit val formats: DefaultFormats = DefaultFormats

    implicit val floppyContext: MyFloppyEarsContext = wire[MyFloppyEarsContext]

    override protected val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()

    lazy val actionDef: ActionDef = ActionDef("some-fun", 2)
    lazy val source: Option[WiretapSource] = floppyContext.sourceFor(actionDef)

    val url: String = "https://kalergic.com"

    lazy val actionContext: ActionContext[MyAppRequest] = ActionContext[MyAppRequest](
      actionDef,
      schema,
      createEvent = request => extractUserId => extractSessionId => bytes => {
        val maybeUserId = extractUserId(request)
        val maybeSessionId = extractSessionId(request)
        val requestParams = request.queryString
        val jsValue = Json.parse(bytes.toArray)
        val maybeResult: Option[SomePayload] = Json.fromJson[SomePayload](jsValue).asOpt
        val requestBodyAsJson = request.body.asInstanceOf[AnyContentAsJson]
        Extraction.decompose(MyFloppyEvent(
          _userId = maybeUserId,
          _sessionId = maybeSessionId,
          a = requestParams.get("a").flatMap(_.headOption).map(_.toInt).get,
          b = requestParams.get("b").flatMap(_.headOption).map(identity).get,
          _requestBody = Json.fromJson[SomePayload](requestBodyAsJson.json).get,
          _responseBody = maybeResult
        )).asInstanceOf[JObject]
      }
    )
  }
}

class FloppyEarsActionFunctionSpec
  extends FlatSpec
  with Matchers
  with ScalaFutures
  with UrlBuilderSugar
  with Eventually {

  import FloppyEarsActionFunction._
  import FloppyEarsActionFunctionSpec._

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(50, Millis)))

  "FloppyEarsActionFunction" should "register a schema" in new FloppyEarsActionFunctionFixture {

    val toIntercept: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents)
    val intercepted: ActionBuilder[MyAppRequest, AnyContent] = toIntercept.withFloppyEars(actionContext)
    intercepted shouldNot be theSameInstanceAs toIntercept

    floppyContext.client._schemaRegistrations.size shouldBe 1
    val maybeRegistration: Option[SchemaRegistration] = floppyContext.client._schemaRegistrations.headOption
    maybeRegistration should not be empty
    val registration: SchemaRegistration = maybeRegistration.get
    registration.source shouldBe source.get
    registration.schema shouldBe actionContext.eventSchema
  }

  it should "fail to instantiate and throw an exception if it fails register a schema" in new FloppyEarsActionFunctionFixture {

    floppyContext.client._failRegistration = true

    intercept[Exception] {
      MyActionBuilder(controllerComponents).withFloppyEars(actionContext)
    }
  }

  "invokeBlock" should "report an event if the action succeeds" in new FloppyEarsActionFunctionFixture {

    val toIntercept: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents)
    val intercepted: ActionBuilder[MyAppRequest, AnyContent] = toIntercept.withFloppyEars(actionContext)

    val request: FakeRequest[AnyContentAsJson] = FakeRequest(method = "POST", path = url.withQueryParam("a", "73").withQueryParam("b", "hello")).withJsonBody(Json.toJson(SomePayload(72)))

    // Volatile: will be updated in a different thread
    @volatile var execThread: Option[Thread] = None

    val resultF: Future[Result] = intercepted.invokeBlock[AnyContentAsJson](request, _ => {
      // This is executing asynchronously, therefore execThread had better be thread safe.
      execThread = Some(Thread.currentThread)
      Future.successful {
        Ok(Json.toJson(SomePayload(73)))
      }
    }
    )

    whenReady(resultF) { result =>
      whenReady(result.body.consumeData) { bytes =>
        val jsValue = Json.parse(bytes.toArray)
        val maybeResult: SomePayload = Json.fromJson[SomePayload](jsValue).get
        maybeResult.x shouldBe 73
      }
    }

    eventually {
      floppyContext.client._events.size shouldBe 1
      val event: SendEvent = floppyContext.client._events.head
      event.source shouldBe source.get
      val eventPayload: JObject = Extraction.decompose(
        MyFloppyEvent(
          _userId = Some("dummy-user-id"),
          _sessionId = Some("dummy-session-id"),
          a = 73,
          b = "hello",
          _requestBody = SomePayload(72),
          _responseBody = Some(SomePayload(73))
        )
      ).asInstanceOf[JObject]
      event.event shouldBe eventPayload
      execThread should not be empty
      event.thread should not be execThread.head
    }
  }

  it should "not report an event if the action fails" in new FloppyEarsActionFunctionFixture {
    val toIntercept: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents)
    val intercepted: ActionBuilder[MyAppRequest, AnyContent] = toIntercept.withFloppyEars(actionContext)

    val request: FakeRequest[AnyContentAsJson] = FakeRequest(method = "POST", path = url.withQueryParam("a", "73").withQueryParam("b", "hello")).withJsonBody(Json.toJson(SomePayload(72)))

    val resultF: Future[Result] = intercepted.invokeBlock[AnyContentAsJson](request, _ => {
      Future.failed {
        new Exception("Thrown intentionally by test")
      }
    }
    )

    whenReady(resultF.failed) { e =>
      e.getMessage shouldBe "Thrown intentionally by test"
    }

    // Wait a reasonable amount of time
    Thread.sleep(1500)

    floppyContext.client._events shouldBe empty
  }

  it should "not report an event if the action succeeds but the application returns a non-successful status code" in new FloppyEarsActionFunctionFixture {

    val toIntercept: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents)
    val intercepted: ActionBuilder[MyAppRequest, AnyContent] = toIntercept.withFloppyEars(actionContext)

    val request: FakeRequest[AnyContentAsJson] = FakeRequest(method = "POST", path = url.withQueryParam("a", "73").withQueryParam("b", "hello")).withJsonBody(Json.toJson(SomePayload(72)))

    val resultF: Future[Result] = intercepted.invokeBlock[AnyContentAsJson](request, _ => {
      Future.successful(BadRequest("{}"))
    }
    )

    whenReady(resultF) { _ =>
      // Wait a reasonable amount of time
      Thread.sleep(1500)
      floppyContext.client._events shouldBe empty
    }
  }

  it should "handle failures to report FloppyEars events without affecting the user flow" in new FloppyEarsActionFunctionFixture {

    floppyContext.client._failSendEvent = true

    val toIntercept: ActionBuilder[MyAppRequest, AnyContent] = MyActionBuilder(controllerComponents)
    val intercepted: ActionBuilder[MyAppRequest, AnyContent] = toIntercept.withFloppyEars(actionContext)

    val request: FakeRequest[AnyContentAsJson] = FakeRequest(method = "POST", path = url.withQueryParam("a", "73").withQueryParam("b", "hello")).withJsonBody(Json.toJson(SomePayload(72)))

    val resultF: Future[Result] = intercepted.invokeBlock[AnyContentAsJson](request, _ => {
      // This is executing asynchronously, therefore execThread had better be thread safe.
      Future.successful {
        Ok(Json.toJson(SomePayload(73)))
      }
    }
    )

    whenReady(resultF) { result =>
      whenReady(result.body.consumeData) { bytes =>
        val jsValue = Json.parse(bytes.toArray)
        val maybeResult: SomePayload = Json.fromJson[SomePayload](jsValue).get
        maybeResult.x shouldBe 73
      }
    }

    // Wait a reasonable amount of time
    Thread.sleep(1500)

    floppyContext.client._events shouldBe empty
  }

}
