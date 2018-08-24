package com.softwaremill.sttp.playJson

import com.softwaremill.sttp.{
  ApplicationJsonContentType,
  MappedResponseAs,
  RequestT,
  ResponseAs,
  ResponseAsString,
  StringBody,
  Utf8,
  contentTypeWithEncoding,
  sttp
}
import org.scalatest.{EitherValues, FlatSpec, Matchers}
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

class PlayJsonSpec extends FlatSpec with Matchers with EitherValues {

  import PlayJsonTests._

  "The playJson module" should "encode arbitrary json bodies" in {
    val body = Outer(Inner(42, b = true, "horses"), "cats")
    val expected = """{"foo":{"a":42,"b":true,"c":"horses"},"bar":"cats"}"""

    val req = sttp.body(body)

    extractBody(req) shouldBe expected
  }

  it should "decode arbitrary bodies" in {
    val body = """{"foo":{"a":42,"b":true,"c":"horses"},"bar":"cats"}"""
    val expected = Outer(Inner(42, b = true, "horses"), "cats")

    val responseAs = asJson[Outer]

    runJsonResponseAs(responseAs)(body) shouldBe JsSuccess(expected)
  }

  it should "decode arbitary bodies as option" in {
    val body = """{"foo":{"a":42,"b":true,"c":"horses"},"bar":"cats"}"""
    val expected = Outer(Inner(42, b = true, "horses"), "cats")
    val responseAs = asJsonOpt[Outer]

    runJsonResponseAs(responseAs)(body) shouldBe Some(expected)
  }

  it should "decode arbitary bodies as either" in {
    val body = """{"foo":{"a":42,"b":true,"c":"horses"},"bar":"cats"}"""
    val expected = Outer(Inner(42, b = true, "horses"), "cats")
    val responseAs = asEither[Outer]

    runJsonResponseAs(responseAs)(body) shouldBe Right(expected)
  }

  it should "decode arbitary bodies as either when there is a failure" in {

    case class MyError()

    val body = """{"foo": "bar"}"""
    val responseAs = asEither[MyError, Outer](_ => MyError())

    runJsonResponseAs(responseAs)(body) shouldBe Left(MyError())
  }

  it should "fail to decode invalid json" in {
    val body = """not valid json"""

    val responseAs = asJson[Outer]

    runJsonResponseAs(responseAs)(body) shouldBe a[JsError]
  }

  it should "set the content type" in {
    val body = Outer(Inner(42, b = true, "horses"), "cats")
    val req = sttp.body(body)

    val ct = req.headers.toMap.get("Content-Type")

    ct shouldBe Some(contentTypeWithEncoding(ApplicationJsonContentType, Utf8))
  }

  def extractBody[A[_], B, C](request: RequestT[A, B, C]): String =
    request.body match {
      case StringBody(body, "utf-8", Some(ApplicationJsonContentType)) =>
        body
      case wrongBody =>
        fail(s"Request body does not serialize to correct StringBody: $wrongBody")
    }

  def runJsonResponseAs[A](responseAs: ResponseAs[A, Nothing]): String => A =
    responseAs match {
      case responseAs: MappedResponseAs[_, A, Nothing] =>
        responseAs.raw match {
          case ResponseAsString("utf-8") =>
            responseAs.g
          case ResponseAsString(encoding) =>
            fail(s"MappedResponseAs wraps a ResponseAsString with wrong encoding: $encoding")
          case _ =>
            fail("MappedResponseAs does not wrap a ResponseAsString")
        }
      case _ => fail("ResponseAs is not a MappedResponseAs")
    }
}

object PlayJsonTests {
  case class Inner(a: Int, b: Boolean, c: String)
  case class Outer(foo: Inner, bar: String)

  implicit val innerFormat: Format[Inner] = Json.format
  implicit val outerFormat: Format[Outer] = Json.format
}
