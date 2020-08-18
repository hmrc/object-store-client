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
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreRead
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing, ObjectMetadata}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlayObjectStoreReads @Inject()(implicit ec: ExecutionContext) extends ObjectStoreRead[Future, WSResponse] {
  override def toObjectListing(response: WSResponse): Future[ObjectListing] =
    response match {
      case r if Status.isSuccessful(r.status) => Future.successful(r.body[JsValue].as[ObjectListing](PlayFormats.objectListingFormat))
      case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
    }

  override def toObject[CONTENT](location: String, response: WSResponse, readContent: WSResponse => Future[CONTENT]): Future[Option[Object[CONTENT]]] =
    response match {
      case r if Status.isSuccessful(r.status) => readContent(r).map { c =>
        def header(k: String) =
          r.header(k).getOrElse(sys.error(s"Missing header $k"))// TODO raise error as non-UpstreamErrorResponse

        Some(Object(
          location = location,
          content  = c,
          metadata = ObjectMetadata(
            contentType   = r.contentType,
            contentLength = header("Content-Length").toLong,
            contentMd5    = header("Content-MD5"),
            lastModified  = ZonedDateTime.parse(header("Last-Modified"), RFC_1123_DATE_TIME).toInstant,
            userMetadata  = Map.empty[String, String] // TODO userMetadata?
          )))
      }
      case r if r.status == Status.NOT_FOUND => Future.successful(None)
      case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
    }

  override def consume(response: WSResponse): Future[Unit] =
    response match {
      case r if Status.isSuccessful(r.status) => Future.successful(())
      case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
    }
}
