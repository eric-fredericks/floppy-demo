package org.kalergic.floppyears.plugintest

import com.softwaremill.macwire._
import org.json4s.{Extraction, JObject}
import org.kalergic.floppyears.plugintest.foo.SomeId
import org.kalergic.floppyears.wiretap.FakeFloppyEarsClient.SchemaRegistration
import org.kalergic.floppyears.wiretap.{
  MyFloppyEarsContext,
  SinglePlayAppForAll,
  UrlBuilderSugar,
  WiretapSource
}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import play.api.libs.typedmap._
import play.api.mvc.{ControllerComponents, EssentialAction, PlayBodyParsers}
import play.api.routing.{HandlerDef, Router}
import play.api.test.{FakeRequest, Helpers}

// This code relies on the SampleControllerImpl, which is in the test source, using code generated from annotations on the methods of the
// SampleController trait. The generator executes against the SampleController source as input and generates code that the implementation
// relies on to instantiate the action functions that use the generated code. These tests should cover all "sunny day" outcomes of the
// execution of the code generator. Cases where the generator produces an error must be tested in the codegen subproject.
class GeneratedCodeUsageSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with Eventually
    with SinglePlayAppForAll
    with UrlBuilderSugar {

  import SampleControllerFloppyEarsSupport._
  import play.api.test.Helpers._

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = scaled(Span(3000, Millis)),
      interval = scaled(Span(100, Millis))
    )

  class GeneratedCodeFixture {
    val urlBase = "https://kalergic.com"
    val playBodyParsers: PlayBodyParsers = PlayBodyParsers()
    val controllerComponents: ControllerComponents =
      Helpers.stubControllerComponents(bodyParser = playBodyParsers.anyContent)
    implicit val floppyEarsContext: MyFloppyEarsContext =
      wire[MyFloppyEarsContext]
    val controller: SampleController = wire[SampleControllerImpl]

    def handlerDef(path: String): HandlerDef = HandlerDef(
      classLoader = null,
      routerPackage = null,
      controller = null,
      method = null,
      parameterTypes = null,
      verb = null,
      path = path
    )
  }

  "The code generator" should "generate code that a controller can use to compose an action function and Floppy Ears schema registration should happen automatically" in new GeneratedCodeFixture {

    floppyEarsContext.client._schemaRegistrations.size shouldBe 6

    // Scope for testing getMethodWithResponse registration
    {
      import GetWithResponseV4Event._
      val source: WiretapSource =
        floppyEarsContext.sourceFor(GetWithResponseV4Action).get
      val maybeRegistration: Option[SchemaRegistration] =
        floppyEarsContext.client._schemaRegistrations.find(_.source == source)
      maybeRegistration should not be empty
      val registration: SchemaRegistration = maybeRegistration.get
      registration.schema shouldBe GetWithResponseV4EventSchema
    }

    // Scope for testing postMethodWithRequest registration
    {
      import PostWithRequestV3Event._
      val source: WiretapSource =
        floppyEarsContext.sourceFor(PostWithRequestV3Action).get
      val maybeRegistration: Option[SchemaRegistration] =
        floppyEarsContext.client._schemaRegistrations.find(_.source == source)
      maybeRegistration should not be empty
      val registration: SchemaRegistration = maybeRegistration.get
      registration.schema shouldBe PostWithRequestV3EventSchema
    }

    // Scope for testing postMethodWithRequestAndResponse registration
    {
      import PostWithRequestAndResponseV2Event._
      val source: WiretapSource =
        floppyEarsContext.sourceFor(PostWithRequestAndResponseV2Action).get
      val maybeRegistration: Option[SchemaRegistration] =
        floppyEarsContext.client._schemaRegistrations.find(_.source == source)
      maybeRegistration should not be empty
      val registration: SchemaRegistration = maybeRegistration.get
      registration.schema shouldBe PostWithRequestAndResponseV2EventSchema
    }

    // Scope for getMethodWithNoParams registration
    {
      import GetWithNoParamV1Event._
      val source: WiretapSource =
        floppyEarsContext.sourceFor(GetWithNoParamV1Action).get
      val maybeRegistration: Option[SchemaRegistration] =
        floppyEarsContext.client._schemaRegistrations.find(_.source == source)
      maybeRegistration should not be empty
      val registration: SchemaRegistration = maybeRegistration.get
      registration.schema shouldBe GetWithNoParamV1EventSchema
    }

    // Scope for postMethodWithTransformedRequestAndResponse registration
    {
      import PostWithTransformedRequestAndResponseV1Event._
      val source: WiretapSource = floppyEarsContext
        .sourceFor(PostWithTransformedRequestAndResponseV1Action)
        .get
      val maybeRegistration: Option[SchemaRegistration] =
        floppyEarsContext.client._schemaRegistrations.find(_.source == source)
      maybeRegistration should not be empty
      val registration: SchemaRegistration = maybeRegistration.get
      registration.schema shouldBe PostWithTransformedRequestAndResponseV1EventSchema
    }

    // Scope for anotherMethodPostWithParsedBody registration
    {
      import AnotherPostWithParsedBodyV1Event._
      val source: WiretapSource =
        floppyEarsContext.sourceFor(AnotherPostWithParsedBodyV1Action).get
      val maybeRegistration: Option[SchemaRegistration] =
        floppyEarsContext.client._schemaRegistrations.find(_.source == source)
      maybeRegistration should not be empty
      val registration: SchemaRegistration = maybeRegistration.get
      registration.schema shouldBe AnotherPostWithParsedBodyV1EventSchema
    }
  }

  it should "generate a FloppyEars event for a non-ignored GET endpoint controller method producing a response body, ignoring any method parameters annotated as ignored" in new GeneratedCodeFixture {

    import GetWithResponseV4Event._

    val essentialAction: EssentialAction = controller.getMethodWithResponse(
      someId = SomeId("73"),
      fooBar = Some(true),
      baz = Some(true),
      bing = true,
      strs1 = Seq("abc", "def", "ghi"),
      strs2 = Seq("jkl", "mno", "pqr"),
      strs3 = Seq("stu", "vwx", "yz"),
      ints = Seq(1, 2, 3),
      ints1 = Seq(4, 5, 6),
      ids = List(SomeId("hello"), SomeId("world")),
      dependentId = SomeId("74"),
      otherId = Some(SomeId("75")),
      aThirdId = Some(SomeId("77")),
      aFourthId = Some(SomeId("78")),
      id = 76,
      ln = 760,
      fl = 3.14f,
      db = 9.9999,
      str = "franco"
    )

    val path: String = (urlBase + "/rest/sample/v1/getmethodwithresponse/74")
      .withQueryParam("someId", "73")
      .withQueryParam("otherId", "75")
      .withQueryParam("id", "76")
      .withQueryParam("aThirdId", "77")
      .withQueryParam("aFourthId", "78")
      .withQueryParam("fooBar", "true")
      .withQueryParam("baz", "true")
      .withQueryParam("bing", "true")
      .withQueryParam("strs1", "abc")
      .withQueryParam("strs1", "def")
      .withQueryParam("strs1", "ghi")
      .withQueryParam("strs2", "jkl")
      .withQueryParam("strs2", "mno")
      .withQueryParam("strs2", "pqr")
      .withQueryParam("strs3", "stu")
      .withQueryParam("strs3", "vwx")
      .withQueryParam("strs3", "yz")
      .withQueryParam("ints", "1")
      .withQueryParam("ints", "2")
      .withQueryParam("ints", "3")
      .withQueryParam("ints1", "4")
      .withQueryParam("ints1", "5")
      .withQueryParam("ints1", "6")
      .withQueryParam("ids", "hello")
      .withQueryParam("ids", "world")
      .withQueryParam("ln", "760")
      .withQueryParam("fl", "3.14")
      .withQueryParam("db", "9.9999")
      .withQueryParam("str", "franco")
    val template = FakeRequest(method = "GET", path = path)
    val handlerDef: HandlerDef = handlerDef(path =
      "rest/sample/v1/getmethodwithresponse/$dependentId<[^/]+>"
    )
    val request = FakeRequest(
      method = template.method,
      uri = template.uri,
      headers = template.headers,
      body = template.body,
      attrs = template.attrs + TypedEntry(Router.Attrs.HandlerDef, handlerDef)
    )

    call(essentialAction, request)

    eventually {
      val event = floppyEarsContext.client._events.head
      event.source shouldBe floppyEarsContext
        .sourceFor(GetWithResponseV4Action)
        .get
      val jObjectPayload: JObject = Extraction
        .decompose(
          GetWithResponseV4Event(
            _userId = Some("dummy-user-id"),
            _sessionId = Some("dummy-session-id"),
            fooBar = Some(true),
            baz = Some(true),
            bing = true,
            strs1 = Seq("abc", "def", "ghi"),
            strs2 = Seq("jkl", "mno", "pqr"),
            strs3 = Seq("stu", "vwx", "yz"),
            ints = Seq(1, 2, 3),
            ints1 = Seq(4, 5, 6),
            ids = List(SomeId("hello"), SomeId("world")),
            dependentId = SomeId("74"),
            otherId = Some(SomeId("75")),
            aThirdId = Some(SomeId("77")),
            aFourthId = Some(SomeId("78")),
            id = 76,
            ln = 760,
            fl = 3.14f,
            db = 9.9999,
            str = "franco",
            _response = SampleData(1)
          )
        )
        .asInstanceOf[JObject]
      event.event shouldBe jObjectPayload
    }
  }

  it should "generate a FloppyEars event for a non-ignored POST endpoint controller method taking a request body, ignoring any method parameters annotated as ignored" in new GeneratedCodeFixture {

    import PostWithRequestV3Event._

    val essentialAction: EssentialAction = controller.postMethodWithRequest(
      str1 = "abc",
      int1 = 123,
      str2 = Some("ghi")
    )

    call(
      essentialAction,
      FakeRequest(
        method = "POST",
        path = (urlBase + "/rest/sample/v1/postmethodwithrequest")
          .withQueryParam("str1", "abc")
          .withQueryParam("int1", "123")
          .withQueryParam("str2", "ghi")
      ).withJsonBody(Json.toJson(SampleInput(19, "nineteen")))
    )

    eventually {
      val event = floppyEarsContext.client._events.head
      event.source shouldBe floppyEarsContext
        .sourceFor(PostWithRequestV3Action)
        .get
      val jObjectPayload: JObject = Extraction
        .decompose(
          PostWithRequestV3Event(
            _userId = Some("dummy-user-id"),
            _sessionId = Some("dummy-session-id"),
            str1 = "abc",
            str2 = Some("ghi"),
            _request = SampleInput(19, "nineteen")
          )
        )
        .asInstanceOf[JObject]
      event.event shouldBe jObjectPayload
    }
  }

  it should "generate a FloppyEars event for a non-ignored POST endpoint controller method taking a request body and producing a response body" in new GeneratedCodeFixture {

    import PostWithRequestAndResponseV2Event._

    val essentialAction: EssentialAction =
      controller.postMethodWithRequestAndResponse(
        str = "wxyz"
      )

    val request = FakeRequest(
      method = "POST",
      path = (urlBase + "/rest/sample/v1/postmethodwithrequestandresponse")
        .withQueryParam("str", "wxyz")
    ).withJsonBody(Json.toJson(SampleInput(20, "twenty")))

    call(essentialAction, request)

    eventually {
      val event = floppyEarsContext.client._events.head
      event.source shouldBe floppyEarsContext
        .sourceFor(PostWithRequestAndResponseV2Action)
        .get
      val jObjectPayload: JObject = Extraction
        .decompose(
          PostWithRequestAndResponseV2Event(
            _userId = Some("dummy-user-id"),
            _sessionId = Some("dummy-session-id"),
            str = "wxyz",
            _request = SampleInput(20, "twenty"),
            _response = SampleData(15)
          )
        )
        .asInstanceOf[JObject]
      event.event shouldBe jObjectPayload
    }

  }

  it should "generate a FloppyEars event for a non-ignored GET method with no parameters producing a response body" in new GeneratedCodeFixture {

    import GetWithNoParamV1Event._

    val essentialAction: EssentialAction = controller.getMethodWithNoParam

    call(
      essentialAction,
      FakeRequest(
        method = "GET",
        path = urlBase + "/rest/sample/v1/getmethodwithnoparam"
      )
    )

    eventually {
      val event = floppyEarsContext.client._events.head
      event.source shouldBe floppyEarsContext
        .sourceFor(GetWithNoParamV1Action)
        .get
      val jObjectPayload: JObject = Extraction
        .decompose(
          GetWithNoParamV1Event(
            _userId = Some("dummy-user-id"),
            _sessionId = Some("dummy-session-id"),
            _response = SampleData(861)
          )
        )
        .asInstanceOf[JObject]
      event.event shouldBe jObjectPayload
    }
  }

  it should "generate a FloppyEars event and without regard to query string parameter name case" in new GeneratedCodeFixture {

    import GetWithResponseV4Event._

    val essentialAction: EssentialAction = controller.getMethodWithResponse(
      someId = SomeId("73"),
      fooBar = None,
      baz = None,
      bing = false,
      strs1 = Seq("abc", "def", "ghi"),
      strs2 = Seq("jkl", "mno", "pqr"),
      strs3 = Seq("stu", "vwx", "yz"),
      ints = Seq(1, 2, 3),
      ints1 = Seq(4, 5, 6),
      ids = List(SomeId("hello"), SomeId("world")),
      dependentId = SomeId("74"),
      otherId = Some(SomeId("75")),
      aThirdId = Some(SomeId("77")),
      aFourthId = Some(SomeId("78")),
      id = 76,
      ln = 760,
      fl = 3.14f,
      db = 9.9999,
      str = "franco"
    )
    // Use different casing for query string parameters
    val path: String = (urlBase + "/rest/sample/v1/getmethodwithresponse/74")
      .withQueryParam("soMeId", "73")
      .withQueryParam("oTheRId", "75")
      .withQueryParam("id", "76")
      .withQueryParam("aThirdId", "77")
      .withQueryParam("aFourthId", "78")
      .withQueryParam("bing", "false")
      .withQueryParam("strs1", "abc")
      .withQueryParam("strs1", "def")
      .withQueryParam("strs1", "ghi")
      .withQueryParam("strs2", "jkl")
      .withQueryParam("strs2", "mno")
      .withQueryParam("strs2", "pqr")
      .withQueryParam("strs3", "stu")
      .withQueryParam("strs3", "vwx")
      .withQueryParam("strs3", "yz")
      .withQueryParam("ints", "1")
      .withQueryParam("ints", "2")
      .withQueryParam("ints", "3")
      .withQueryParam("ints1", "4")
      .withQueryParam("ints1", "5")
      .withQueryParam("ints1", "6")
      .withQueryParam("ids", "hello")
      .withQueryParam("ids", "world")
      .withQueryParam("ln", "760")
      .withQueryParam("fl", "3.14")
      .withQueryParam("db", "9.9999")
      .withQueryParam("str", "franco")

    val template = FakeRequest(method = "GET", path = path)
    val handlerDef: HandlerDef = handlerDef(path =
      "rest/sample/v1/getmethodwithresponse/$dependentId<[^/]+>"
    )
    val request = FakeRequest(
      method = template.method,
      uri = template.uri,
      headers = template.headers,
      body = template.body,
      attrs = template.attrs + TypedEntry(Router.Attrs.HandlerDef, handlerDef)
    )

    call(essentialAction, request)

    eventually {
      val event = floppyEarsContext.client._events.head
      event.source shouldBe floppyEarsContext
        .sourceFor(GetWithResponseV4Action)
        .get
      val jObjectPayload: JObject = Extraction
        .decompose(
          GetWithResponseV4Event(
            _userId = Some("dummy-user-id"),
            _sessionId = Some("dummy-session-id"),
            fooBar = None,
            baz = None,
            bing = false,
            strs1 = Seq("abc", "def", "ghi"),
            strs2 = Seq("jkl", "mno", "pqr"),
            strs3 = Seq("stu", "vwx", "yz"),
            ints = Seq(1, 2, 3),
            ints1 = Seq(4, 5, 6),
            ids = List(SomeId("hello"), SomeId("world")),
            dependentId = SomeId("74"),
            otherId = Some(SomeId("75")),
            aThirdId = Some(SomeId("77")),
            aFourthId = Some(SomeId("78")),
            id = 76,
            ln = 760,
            fl = 3.14f,
            db = 9.9999,
            str = "franco",
            _response = SampleData(1)
          )
        )
        .asInstanceOf[JObject]
      event.event shouldBe jObjectPayload
    }
  }

  it should "generate a FloppyEars event for a non-ignored POST endpoint controller method taking a JSON request body and producing a JSON response body where the request and response bodies are transformed" in new GeneratedCodeFixture {

    import PostWithTransformedRequestAndResponseV1Event._

    val essentialAction: EssentialAction =
      controller.postMethodWithTransformedRequestAndResponse

    val request = FakeRequest(
      method = "POST",
      path = urlBase + "/rest/sample/v1/postwithtransformations"
    ).withJsonBody(Json.toJson(SampleInput(20, "twenty")))
    call(essentialAction, request)

    eventually {
      val event = floppyEarsContext.client._events.head
      event.source shouldBe floppyEarsContext
        .sourceFor(PostWithTransformedRequestAndResponseV1Action)
        .get
      val jObjectPayload: JObject = Extraction
        .decompose(
          PostWithTransformedRequestAndResponseV1Event(
            _userId = Some("dummy-user-id"),
            _sessionId = Some("dummy-session-id"),
            _request = SampleData(20),
            _response = SampleData(432) // the sample data output was 431 but the floppy ears code should have transformed it to 432.
          )
        )
        .asInstanceOf[JObject]
      event.event shouldBe jObjectPayload
    }

  }

  it should "generate a FloppyEars event for a non-ignored POST endpoint controller method with a BodyParser parsed request body and producing a JSON response body where the request and response bodies are transformed" in new GeneratedCodeFixture {

    import AnotherPostWithParsedBodyV1Event._

    val essentialAction: EssentialAction =
      controller.anotherPostMethodWithParsedBodyAndTransformations

    call(
      essentialAction,
      FakeRequest(
        method = "POST",
        path = urlBase + "/rest/sample/v1/anotherpostwithtransformations"
      ).withJsonBody(Json.toJson(SampleInput(20, "twenty")))
    )

    eventually {
      val event = floppyEarsContext.client._events.head
      event.source shouldBe floppyEarsContext
        .sourceFor(AnotherPostWithParsedBodyV1Action)
        .get
      val jObjectPayload: JObject = Extraction
        .decompose(
          AnotherPostWithParsedBodyV1Event(
            _userId = Some("dummy-user-id"),
            _sessionId = Some("dummy-session-id"),
            _request = SampleData(20),
            _response = SampleData(356) // the sample data output was 355 but the floppy ears code should have transformed it to 356.
          )
        )
        .asInstanceOf[JObject]
      event.event shouldBe jObjectPayload
    }
  }
}
