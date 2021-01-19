/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.{Json, JsError, JsSuccess}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.objectstore.client.http.ObjectStoreRead
import uk.gov.hmrc.objectstore.client.{Object, ObjectListing, ObjectMetadata}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PlayObjectStoreReads {

  def futureEitherReads(implicit m: Materializer, ec: ExecutionContext): ObjectStoreRead[FutureEither, Response, Source[ByteString, NotUsed]] =
    new ObjectStoreRead[FutureEither, Response, Source[ByteString, NotUsed]] {
      override def toObjectListing(response: Response): FutureEither[ObjectListing] =
        response match {
          case r if Status.isSuccessful(r.status) =>
            r.bodyAsSource.map(_.utf8String).runReduce(_ + _).map { str =>
              Json.parse(str).validate[ObjectListing](PlayFormats.objectListingRead) match {
                case JsSuccess(r, _) => Right(r)
                case JsError(errors) =>
                  Left(
                    new RuntimeException(
                      s"Attempt to convert json to ${classOf[ObjectListing].getName} gave errors: $errors"
                    )
                  )
              }
            }
          case r => Future.successful(Left(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}, body: ${r.body[String]}", r.status)))
        }

      override def toObject(location: String, response: Response): FutureEither[Option[Object[ResBody]]] =
        Future.successful(
          response match {
            case resp @ r if Status.isSuccessful(r.status) =>
              def header(k: String): Either[Exception, String] =
                r.header(k).map(Right(_)).getOrElse(Left(new RuntimeException(s"Missing header $k")))

              def attempt[A](h: String, v: => A): Either[Exception, A] =
                Try(v) match {
                  case Success(s) => Right(s)
                  case Failure(e) => Left(new RuntimeException(s"Couldn't read header $h: ${e.getMessage}"))
                }

              for {
                cl            <- header("Content-Length").right
                contentLength <- attempt("Content-Length", cl.toLong).right
                contentMd5    <- header("Content-MD5").right
                lm            <- header("Last-Modified").right
                lastModified  <- attempt("Last-Modified", ZonedDateTime.parse(lm, RFC_1123_DATE_TIME).toInstant).right
              } yield Some(
                Object(
                  location = location,
                  content = resp.bodyAsSource.mapMaterializedValue(_ => NotUsed),
                  metadata = ObjectMetadata(
                    contentType = r.contentType,
                    contentLength = contentLength,
                    contentMd5 = contentMd5,
                    lastModified = lastModified,
                    userMetadata = Map.empty[String, String] // TODO userMetadata?
                  )
                )
              )

            case r if r.status == Status.NOT_FOUND => Right(None)
            case r                                 => Left(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}, body: ${r.body[String]}", r.status))
          }
        )

      override def consume(response: Response): FutureEither[Unit] =
        Future.successful(
          response match {
            case r if Status.isSuccessful(r.status) => Right(())
            case r                                  => Left(UpstreamErrorResponse(s"Object store call failed with status code: ${r.status}, body: ${r.body[String]}", r.status))
          }
        )
    }

  def futureReads(implicit m: Materializer, ec: ExecutionContext): ObjectStoreRead[Future, Response, Source[ByteString, NotUsed]] =
    new ObjectStoreRead[Future, Response, Source[ByteString, NotUsed]] {

      private def transform[A](f: FutureEither[A]): Future[A] =
        f.flatMap {
          case Right(a) => Future.successful(a)
          case Left(e)  => Future.failed(e)
        }

      override def toObjectListing(response: Response): Future[ObjectListing] =
        transform(futureEitherReads.toObjectListing(response))

      override def toObject(location: String, response: Response): Future[Option[Object[Source[ByteString, NotUsed]]]] =
        transform(futureEitherReads.toObject(location, response))

      override def consume(response: Response): Future[Unit] =
        transform(futureEitherReads.consume(response))
    }
}
