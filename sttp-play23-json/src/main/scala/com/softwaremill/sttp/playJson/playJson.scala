package com.softwaremill.sttp

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

package object playJson {
  implicit def playJsonBodySerializer[B: Writes]: BodySerializer[B] =
    b =>
      StringBody(Json.stringify(implicitly[Writes[B]].writes(b)),
        Utf8,
        Some(ApplicationJsonContentType))

  def asJsonOpt[B: Reads]: ResponseAs[Option[B], Nothing] =
    asJson[B].map(_.asOpt)

  def as[B: Reads]: ResponseAs[B, Nothing] =
    asJson[B].map(_.get)

  def asEither[B: Reads]: ResponseAs[Either[JsError, B], Nothing] =
    asJson[B].map(_.asEither.left.map(JsError(_)))

  def asEither[C, B: Reads](onError: JsError => C): ResponseAs[Either[C, B], Nothing] =
    asJson[B].map(_.asEither.left.map(errors => onError(JsError(errors))))

  def asJson[B: Reads]: ResponseAs[JsResult[B], Nothing] =
    asString(Utf8).map(
      s =>
        if (s.nonEmpty)
          Try(Json.parse(s)).map(implicitly[Reads[B]].reads(_)) match {
            case Success(jsSuccess) => jsSuccess
            // Something went wrong during parsing
            case Failure(e) => JsError(__, e.getMessage)
          } else JsError(__, "Empty string could not be parsed")
    )
}
