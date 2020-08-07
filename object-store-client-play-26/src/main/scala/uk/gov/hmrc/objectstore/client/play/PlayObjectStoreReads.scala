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

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.Status
import play.api.libs.json.{JsResult, JsValue, Json, Reads}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.objectstore.client.model.MonadError
import uk.gov.hmrc.objectstore.client.model.http.{ObjectStoreRead, ObjectStoreContentRead}
import uk.gov.hmrc.objectstore.client.model.objectstore
import uk.gov.hmrc.objectstore.client.model.objectstore.ObjectListing

import scala.concurrent.{ExecutionContext, Future}

trait PlayObjectStoreReads {

  private def wsResponseRead[F[_], E >: UpstreamErrorResponse](implicit F: MonadError[F, E]): ObjectStoreRead[WSResponse, F] =
    new ObjectStoreRead[WSResponse, F]{
      override def toObjectListing(response: WSResponse): F[ObjectListing] =
        response match {
          case r if Status.isSuccessful(r.status) => F.pure(r.body[JsValue].as[ObjectListing](PlayFormats.objectListingFormat))
          case r => F.raiseError(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def toObject(response: WSResponse): F[Option[objectstore.Object[WSResponse]]] =
        response match {
          case r if Status.isSuccessful(r.status) => F.pure(Some(objectstore.Object("", response))) // todo - location is empty?
          case r if r.status == Status.NOT_FOUND => F.pure(None)
          case r => F.raiseError(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def consume(response: WSResponse): F[Unit] =
        response match {
          case r if Status.isSuccessful(r.status) => F.pure(())
          case r => F.raiseError(UpstreamErrorResponse("Object store call failed", r.status))
        }
    }

  implicit def futureRead(implicit ec: ExecutionContext): ObjectStoreRead[WSResponse, Future] =
    wsResponseRead[Future, Exception]

  // TODO however this is probably not useful, since it can only used with a ObjectStoreClient which operates with Either
  // in which case, we may as well just replace F with Future in common?
  implicit val eitherRead: ObjectStoreRead[WSResponse, Either[UpstreamErrorResponse, *]] =
    wsResponseRead[Either[UpstreamErrorResponse, *], UpstreamErrorResponse]
}

object PlayObjectStoreReads extends PlayObjectStoreReads

trait PlayObjectStoreContentReads {

  implicit def akkaSourceContentRead: ObjectStoreContentRead[WSResponse, Source[ByteString, NotUsed]] =
    new ObjectStoreContentRead[WSResponse, Source[ByteString, NotUsed]]{
      override def readContent(response: WSResponse): Source[ByteString, NotUsed] =
        response.bodyAsSource.mapMaterializedValue(_ => NotUsed)
    }
}

trait InMemoryPlayObjectStoreContentReads extends PlayObjectStoreContentReads {
  implicit val stringContentRead: ObjectStoreContentRead[WSResponse, String] =
    new ObjectStoreContentRead[WSResponse, String]{
      override def readContent(response: WSResponse): String =
        response.body
    }

  implicit val jsValueContentRead: ObjectStoreContentRead[WSResponse, JsValue] =
    stringContentRead.map(Json.parse)

  implicit def jsResultContentRead[A : Reads]: ObjectStoreContentRead[WSResponse, JsResult[A]] =
    jsValueContentRead.map(_.validate[A])
}

object PlayObjectStoreContentReads extends PlayObjectStoreContentReads

case class UpstreamErrorResponse(message: String, statusCode: Int) extends Exception(message)
