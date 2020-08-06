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
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.objectstore.client.model.http.{ObjectStoreRead, ObjectStoreContentRead}
import uk.gov.hmrc.objectstore.client.model.objectstore
import uk.gov.hmrc.objectstore.client.model.objectstore.ObjectListing

import scala.concurrent.{ExecutionContext, Future}

trait PlayObjectStoreReads {

  implicit def futureRead(implicit ec: ExecutionContext): ObjectStoreRead[WSResponse, Future] =
    new ObjectStoreRead[WSResponse, Future]{

      override def toObjectListing(response: WSResponse): Future[ObjectListing] =
        response match {
          case r if Status.isSuccessful(r.status) => Future.successful(r.body[JsValue].as[ObjectListing](PlayFormats.objectListingFormat))
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def toObject(response: WSResponse): Future[Option[objectstore.Object[WSResponse]]] =
        response match {
          case r if Status.isSuccessful(r.status) => Future.successful(Some(objectstore.Object("", response))) // todo - location is empty?
          case r if r.status == Status.NOT_FOUND => Future.successful(None)
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def consume(response: WSResponse): Future[Unit] =
        response match {
          case r if Status.isSuccessful(r.status) => Future.successful(())
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }
    }
}

object PlayObjectStoreReads extends PlayObjectStoreReads

trait PlayObjectStoreContentReads {

  implicit def futureAkkaSourceContentRead(implicit ec: ExecutionContext): ObjectStoreContentRead[WSResponse, Source[ByteString, NotUsed]] =
    new ObjectStoreContentRead[WSResponse, Source[ByteString, NotUsed]]{

      override def readContent(response: WSResponse): Source[ByteString, NotUsed] =
        response.bodyAsSource.mapMaterializedValue(_ => NotUsed)
    }

  // TODO move this so it is imported explicitly, since it will load everything into memory...
  implicit def futureStringContentRead(implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[WSResponse, Future[String]] =
    futureAkkaSourceContentRead.map(_.map(_.utf8String).runReduce(_ + _))
}

object PlayObjectStoreContentReads extends PlayObjectStoreContentReads

case class UpstreamErrorResponse(message: String, statusCode: Int) extends Exception(message)
