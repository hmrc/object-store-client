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
import uk.gov.hmrc.objectstore.client.model.http.{ObjectStoreRead, ObjectStoreRead2}
import uk.gov.hmrc.objectstore.client.model.objectstore
import uk.gov.hmrc.objectstore.client.model.objectstore.ObjectListing

import scala.concurrent.{ExecutionContext, Future}

trait PlayObjectStoreReads {

  implicit def futureRead(implicit ec: ExecutionContext): ObjectStoreRead[Future[WSResponse], Future] =
    new ObjectStoreRead[Future[WSResponse], Future]{

      override def toObjectListing(response: Future[WSResponse]): Future[ObjectListing] =
        response.flatMap {
          case r if Status.isSuccessful(r.status) => Future.successful(r.body[JsValue].as[ObjectListing](PlayFormats.objectListingFormat))
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def toObject(response: Future[WSResponse]): Future[Option[objectstore.Object[Future[WSResponse]]]] =
        response.flatMap {
          case r if Status.isSuccessful(r.status) => Future.successful(Some(objectstore.Object("", response))) // todo - location is empty?
          case r if r.status == Status.NOT_FOUND => Future.successful(None)
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def consume(response: Future[WSResponse]): Future[Unit] =
        response.flatMap {
          case r if Status.isSuccessful(r.status) => Future.successful(())
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }
    }
}

object PlayObjectStoreReads extends PlayObjectStoreReads

trait PlayObjectStoreReads2 {

  implicit def futureAkkaSourceRead2(implicit ec: ExecutionContext): ObjectStoreRead2[Future[WSResponse], Future[Source[ByteString, NotUsed]]] =
    new ObjectStoreRead2[Future[WSResponse], Future[Source[ByteString, NotUsed]]]{

      override def toContent(response: Future[WSResponse]): Future[Source[ByteString, NotUsed]] =
        response.map(_.bodyAsSource.mapMaterializedValue(_ => NotUsed))
    }

  // TODO move this so it is imported explicitly, since it will load everything into memory...
  implicit def futureStringRead2(implicit ec: ExecutionContext, m: Materializer): ObjectStoreRead2[Future[WSResponse], Future[String]] =
    futureAkkaSourceRead2.map(_.flatMap(_.map(_.utf8String).runReduce(_ + _)))
}

object PlayObjectStoreReads2 extends PlayObjectStoreReads2

case class UpstreamErrorResponse(message: String, statusCode: Int) extends Exception(message)
