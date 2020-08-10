/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.objectstore.client.play

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME

import javax.inject.{Inject, Singleton}
import play.api.http.Status
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreRead
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing, ObjectMetadata}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

@Singleton
class PlayObjectStoreReads @Inject()(implicit ec: ExecutionContext) extends ObjectStoreRead[F, WSResponse]{
  override def toObjectListing(response: WSResponse): F[ObjectListing] =
    response match {
      case r if Status.isSuccessful(r.status) => r.body[JsValue].validate[ObjectListing](PlayFormats.objectListingRead) match {
        case JsSuccess(r, path) => F.pure(r)
        case JsError(errors) => F.raiseError(OtherError(errors.toString))
      }
      case r => F.raiseError(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}", r.status))
    }

  override def toObject[CONTENT](location: String, response: WSResponse, readContent: WSResponse => F[CONTENT]): F[Option[Object[CONTENT]]] =
    response match {
      case r if Status.isSuccessful(r.status) =>
        F.flatMap(readContent(r)){ content =>
          def header(k: String): F[String] =
            r.header(k).map(F.pure).getOrElse(F.raiseError(OtherError(s"Missing header $k")))

          def attempt[A](h: String, v: => A): F[A] =
            Try(v) match {
              case Success(s) => F.pure(s)
              case Failure(e) => F.raiseError(OtherError(s"Couldn't read header $h: ${e.getMessage}"))
            }

          F.flatMap(F.flatMap(header("Content-Length"))(cl => attempt("Content-Length", cl.toLong)))(contentLength =>
            F.flatMap(header("Content-MD5"))(contentMd5 =>
              F.map(F.flatMap(header("Last-Modified"))(lm => attempt("Last-Modified", ZonedDateTime.parse(lm, RFC_1123_DATE_TIME).toInstant)))(lastModified =>
                Some(Object(
                location = location,
                content  = content,
                metadata = ObjectMetadata(
                  contentType   = r.contentType,
                  contentLength = contentLength,
                  contentMd5    = contentMd5,
                  lastModified  = lastModified,
                  userMetadata  = Map.empty[String, String] // TODO userMetadata?
                )))
              )
            )
          )
        }
      case r if r.status == Status.NOT_FOUND => F.pure(None)
      case r => F.raiseError(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}", r.status))
    }

  override def consume(response: WSResponse): F[Unit] =
    response match {
      case r if Status.isSuccessful(r.status) => F.pure(())
      case r => F.raiseError(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}", r.status))
    }
}
