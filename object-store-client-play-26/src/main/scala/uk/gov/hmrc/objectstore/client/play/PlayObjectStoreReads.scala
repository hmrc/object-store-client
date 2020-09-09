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

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.Status
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.objectstore.client.{Object, ObjectListing, ObjectMetadata}
import uk.gov.hmrc.objectstore.client.http.ObjectStoreRead

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object PlayObjectStoreReads {

  def futureEitherReads: ObjectStoreRead[FutureEither, Response, Source[ByteString, NotUsed]] =
    new ObjectStoreRead[FutureEither, Response, Source[ByteString, NotUsed]] {
      override def toObjectListing(response: Response): FutureEither[ObjectListing] =
        Future.successful(
          response match {
            case r if Status.isSuccessful(r.status) => r.body[JsValue].validate[ObjectListing](PlayFormats.objectListingRead) match {
              case JsSuccess(r, path) => Right(r)
              case JsError(errors) => Left(GenericError(errors.toString))
            }
            case r => Left(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}", r.status))
          }
        )

      override def toObject(location: String, response: Response): FutureEither[Option[Object[ResBody]]] =
        Future.successful(
          response match {
            case resp@r if Status.isSuccessful(r.status) =>
              def header(k: String): Either[PlayObjectStoreException, String] =
                r.header(k).map(Right(_)).getOrElse(Left(GenericError(s"Missing header $k")))

              def attempt[A](h: String, v: => A): Either[PlayObjectStoreException, A] =
                Try(v) match {
                  case Success(s) => Right(s)
                  case Failure(e) => Left(GenericError(s"Couldn't read header $h: ${e.getMessage}"))
                }

              for {
                cl            <- header("Content-Length").right
                contentLength <- attempt("Content-Length", cl.toLong).right
                contentMd5    <- header("Content-MD5").right
                lm            <- header("Last-Modified").right
                lastModified  <- attempt("Last-Modified", ZonedDateTime.parse(lm, RFC_1123_DATE_TIME).toInstant).right
              } yield
                Some(Object(
                  location = location,
                  content  = resp.bodyAsSource.mapMaterializedValue(_ => NotUsed),
                  metadata = ObjectMetadata(
                    contentType   = r.contentType,
                    contentLength = contentLength,
                    contentMd5    = contentMd5,
                    lastModified  = lastModified,
                    userMetadata  = Map.empty[String, String] // TODO userMetadata?
                  )))

            case r if r.status == Status.NOT_FOUND => Right(None)
            case r => Left(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}", r.status))
          }
        )

      override def consume(response: Response): FutureEither[Unit] =
        Future.successful(
          response match {
            case r if Status.isSuccessful(r.status) => Right(())
            case r => Left(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}", r.status))
          }
        )
  }

  def futureReads(implicit ec: ExecutionContext): ObjectStoreRead[Future, Response, Source[ByteString, NotUsed]] =
    new ObjectStoreRead[Future, Response, Source[ByteString, NotUsed]] {

      private def transform[A](f: FutureEither[A]): Future[A] =
        f.flatMap {
          case Right(a) => Future.successful(a)
          case Left(e) => Future.failed(e)
        }

      override def toObjectListing(response: Response): Future[ObjectListing] =
        transform(futureEitherReads.toObjectListing(response))

      override def toObject(location: String, response: Response): Future[Option[Object[Source[ByteString, NotUsed]]]] =
        transform(futureEitherReads.toObject(location, response))

      override def consume(response: Response): Future[Unit] =
        transform(futureEitherReads.consume(response))
    }
}
