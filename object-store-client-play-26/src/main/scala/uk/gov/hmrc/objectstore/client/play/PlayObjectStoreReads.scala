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

import _root_.play.api.http.Status
import _root_.play.api.libs.json.{JsError, JsSuccess, JsValue}
import _root_.play.api.libs.ws.WSResponse
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreRead
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

object PlayObjectStoreReads {

  //todo CF can we combine these two?
  //WSResponse could be Source
  def future(implicit ec: ExecutionContext): ObjectStoreRead[Future, Future[WSResponse], WSResponse] =

    new ObjectStoreRead[Future, Future[WSResponse], WSResponse]{
      override def toObjectListing(response: Future[WSResponse]): Future[ObjectListing] =
        response.flatMap {
          case r if Status.isSuccessful(r.status) => r.body[JsValue].validate[ObjectListing](PlayFormats.objectListingFormat) match {
            case JsSuccess(r, path) => Future.successful(r)
            case JsError(errors) => Future.failed(OtherError(errors.toString))
          }
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def toObject(response: Future[Response]): Future[Option[Object[WSResponse]]] =
        response.flatMap {
          case r if Status.isSuccessful(r.status) => Future.successful(Some(Object("", r)))// todo - location is empty?
          case r if r.status == Status.NOT_FOUND => Future.successful(None)
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def consume(response: Future[WSResponse]): Future[Unit] = {
        response.flatMap{
          case r if Status.isSuccessful(r.status) => Future.successful(())
          case r => Future.failed(UpstreamErrorResponse("Object store call failed", r.status))
        }
      }
    }

  type FutureEither[A] = Future[Either[PlayObjectStoreException, A]]
   def futureEither[F[_]](implicit ec: ExecutionContext): ObjectStoreRead[FutureEither, Future[WSResponse], WSResponse] =
    new ObjectStoreRead[FutureEither, Future[WSResponse], WSResponse]{
      override def toObjectListing(response: Future[Response]): FutureEither[ObjectListing] = {
      response.map {
          case r if Status.isSuccessful(r.status) => r.body[JsValue].validate[ObjectListing](PlayFormats.objectListingFormat) match {
            case JsSuccess(r, path) => Right(r)
            case JsError(errors) => Left(OtherError(errors.toString))
          }
          case r => Left(UpstreamErrorResponse("Object store call failed", r.status))
        }
      }

      override def toObject(response: Future[Response]): FutureEither[Option[Object[Response]]] =
        response.map {
          case r if Status.isSuccessful(r.status) =>  Right(Some(Object("", r))) // todo - location is empty?
          case r if r.status == Status.NOT_FOUND => Right(None)
          case r => Left(UpstreamErrorResponse("Object store call failed", r.status))
        }

      override def consume(response: Future[Response]): FutureEither[Unit] =
        response.map {
          case r if Status.isSuccessful(r.status) => Right(())
          case r => Left(UpstreamErrorResponse("Object store call failed", r.status))
        }
    }
}
