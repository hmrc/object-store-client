/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json.{JsResult, JsValue, Json, Reads}
import uk.gov.hmrc.objectstore.client.http.ObjectStoreContentRead

import scala.util.{Failure, Success, Try}

trait PlayObjectStoreContentReads {

  implicit def akkaSourceContentRead[F[_]](implicit
    F: PlayMonad[F]
  ): ObjectStoreContentRead[F, ResBody, Source[ByteString, NotUsed]] =
    new ObjectStoreContentRead[F, ResBody, Source[ByteString, NotUsed]] {
      override def readContent(response: ResBody): F[Source[ByteString, NotUsed]] =
        F.pure(response)
    }
}

trait LowPriorityInMemoryPlayObjectStoreContentReads extends PlayObjectStoreContentReads {

  def stringContentRead[F[_]](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, ResBody, String] =
    akkaSourceContentRead.mapF(src => F.liftFuture(src.map(_.utf8String).runReduce(_ + _)))

  def jsValueContentRead[F[_]](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, ResBody, JsValue] =
    stringContentRead.mapF(s =>
      Try(Json.parse(s)) match {
        case Failure(e) => F.raiseError(new RuntimeException(s"Failed to parse Json: ${e.getMessage}"))
        case Success(a) => F.pure(a)
      }
    )

  implicit def jsReadsRead[F[_], A: Reads](implicit
    m: Materializer,
    F: PlayMonad[F]
  ): ObjectStoreContentRead[F, ResBody, A] =
    jsValueContentRead.map(_.as[A])
}

trait InMemoryPlayObjectStoreContentReads extends LowPriorityInMemoryPlayObjectStoreContentReads {

  override implicit def stringContentRead[F[_]](implicit
    m: Materializer,
    F: PlayMonad[F]
  ): ObjectStoreContentRead[F, ResBody, String] =
    super.stringContentRead

  override implicit def jsValueContentRead[F[_]](implicit
    m: Materializer,
    F: PlayMonad[F]
  ): ObjectStoreContentRead[F, ResBody, JsValue] =
    super.jsValueContentRead

  implicit def jsResultContentRead[F[_], A: Reads](implicit
    m: Materializer,
    F: PlayMonad[F]
  ): ObjectStoreContentRead[F, ResBody, JsResult[A]] =
    jsValueContentRead.map(_.validate[A])
}
