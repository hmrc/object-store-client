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
import play.api.libs.json.{Json, JsError, JsSuccess, Reads}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.objectstore.client.http.ObjectStoreRead
import uk.gov.hmrc.objectstore.client.{Md5Hash, Object, ObjectListing, ObjectMetadata, ObjectSummaryWithMd5, Path}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object PlayObjectStoreReads {

  def futureEitherReads(implicit m: Materializer, ec: ExecutionContext): ObjectStoreRead[FutureEither, Response, Source[ByteString, NotUsed]] =
    new ObjectStoreRead[FutureEither, Response, Source[ByteString, NotUsed]] {
      override def toObjectListing(response: Response): FutureEither[ObjectListing] = {
        implicit val osr = PlayFormats.objectListingReads
        toDomain[ObjectListing](response)
      }

      override def toObject(location: String, response: Response): FutureEither[Option[Object[ResBody]]] =
        response.status match {
          case SuccessStatus(_) =>
            def header(k: String): Either[Exception, String] =
              response.header(k).map(Right(_)).getOrElse(Left(new RuntimeException(s"Missing header $k")))

            def attempt[A](h: String, v: => A): Either[Exception, A] =
              Try(v) match {
                case Success(s) => Right(s)
                case Failure(e) => Left(new RuntimeException(s"Couldn't read header $h: ${e.getMessage}"))
              }

            Future.successful {
              for {
                cl            <- header("Content-Length").right
                contentLength <- attempt("Content-Length", cl.toLong).right
                contentMd5    <- header("Content-MD5").map(Md5Hash.apply).right
                lm            <- header("Last-Modified").right
                lastModified  <- attempt("Last-Modified", ZonedDateTime.parse(lm, RFC_1123_DATE_TIME).toInstant).right
              } yield Some(
                Object(
                  location = Path.File(location),
                  content  = response.bodyAsSource.mapMaterializedValue(_ => NotUsed),
                  metadata = ObjectMetadata(
                    contentType   = response.contentType,
                    contentLength = contentLength,
                    contentMd5    = contentMd5,
                    lastModified  = lastModified,
                    userMetadata  = Map.empty[String, String] // TODO userMetadata?
                  )
                )
              )
            }
        case Status.NOT_FOUND =>
          Future.successful(Right(None))
        case _ =>
          readBody(response).map { bodyStr =>
            Left(UpstreamErrorResponse(s"Object store call failed with status code: ${response.status}, body: $bodyStr", response.status))
          }
        }

      override def toObjectSummaryWithMd5(response: Response): FutureEither[ObjectSummaryWithMd5] = {
        implicit val osr = PlayFormats.objectSummaryWithMd5Reads
        toDomain[ObjectSummaryWithMd5](response)
      }

      override def consume(response: Response): FutureEither[Unit] =
        response.status match {
          case SuccessStatus(_) => Future.successful(Right(()))
          case _ =>
            readBody(response).map { bodyStr =>
              Left(UpstreamErrorResponse(s"Object store call failed with status code: ${response.status}, body: $bodyStr", response.status))
            }
        }

      private def toDomain[A](response: Response)(implicit reads: Reads[A], ct: ClassTag[A]): FutureEither[A] =
        response.status match {
          case SuccessStatus(_) =>
            readBody(response).map { bodyStr =>
              Json.parse(bodyStr).validate[A](reads) match {
                case JsSuccess(r, _) => Right(r)
                case JsError(errors) =>
                  Left(
                    new RuntimeException(
                      s"Attempt to convert json to $ct gave errors: $errors"
                    )
                  )
              }
            }
          case _ =>
            readBody(response).map { bodyStr =>
              Left(UpstreamErrorResponse(s"Object store call failed with status code: ${response.status}, body: $bodyStr", response.status))
            }
          }
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

      override def toObjectSummaryWithMd5(response: Response): Future[ObjectSummaryWithMd5] =
        transform(futureEitherReads.toObjectSummaryWithMd5(response))

      override def consume(response: Response): Future[Unit] =
        transform(futureEitherReads.consume(response))
    }

  private def readBody(response: Response)(implicit m: Materializer): Future[String] =
    // runFold rather than runReduce in case stream is empty
    response.bodyAsSource.map(_.utf8String).runFold("")(_ + _)

  private object SuccessStatus {
    def unapply(status: Int): Option[Int] =
      Some(status).filter(Status.isSuccessful)
  }
}
