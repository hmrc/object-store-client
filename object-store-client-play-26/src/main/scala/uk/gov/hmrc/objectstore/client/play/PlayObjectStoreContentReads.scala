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
import play.api.libs.json.{JsResult, JsValue, Json, Reads}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreContentRead
import uk.gov.hmrc.objectstore.client.model.objectstore

import scala.util.{Failure, Success, Try}

trait PlayObjectStoreContentReads {

  implicit def identity[F[_]]: ObjectStoreContentRead[F, Source[ByteString, NotUsed], Source[ByteString, NotUsed]] =
    new ObjectStoreContentRead[F, Source[ByteString, NotUsed], Source[ByteString, NotUsed]] {
      override def readContent(response: F[Option[objectstore.Object[Source[ByteString, NotUsed]]]]): F[Option[objectstore.Object[Source[ByteString, NotUsed]]]] = response
    }

  implicit def akkaSourceContentRead[F[_]](implicit F: PlayMonad[F]): ObjectStoreContentRead[F, WSResponse, Source[ByteString, NotUsed]] =
    new ObjectStoreContentRead[F, WSResponse, Source[ByteString, NotUsed]] {
      override def readContent(response: F[Option[objectstore.Object[WSResponse]]]): F[Option[objectstore.Object[Source[ByteString, NotUsed]]]] = {
        F.map(response)(_.map(obj => obj.copy(content = obj.content.bodyAsSource.mapMaterializedValue(_ => NotUsed))))
      }
    }
}

trait LowPriorityInMemoryPlayObjectStoreContentReads extends PlayObjectStoreContentReads {

  def stringContentRead[F[_]](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, Source[ByteString, NotUsed], String] =
    identity.mapF(src => F.liftFuture(src.map(_.utf8String).runReduce(_ + _)))

  def jsValueContentRead[F[_]](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, Source[ByteString, NotUsed], JsValue] =
    stringContentRead.mapF(s =>
      Try(Json.parse(s)) match {
        case Failure(e) => F.raiseError(OtherError(s"Failed to parse Json: ${e.getMessage}"))
        case Success(a) => F.pure(a)
      }
    )

  implicit def jsReadsRead[F[_], A : Reads](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, Source[ByteString, NotUsed], A] =
    jsValueContentRead.map(_.as[A])
}

trait InMemoryPlayObjectStoreContentReads extends LowPriorityInMemoryPlayObjectStoreContentReads {

  override implicit def stringContentRead[F[_]](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, Source[ByteString, NotUsed], String] =
    super.stringContentRead

  override implicit def jsValueContentRead[F[_]](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, Source[ByteString, NotUsed], JsValue] =
    super.jsValueContentRead

  implicit def jsResultContentRead[F[_], A : Reads](implicit m: Materializer, F: PlayMonad[F]): ObjectStoreContentRead[F, Source[ByteString, NotUsed], JsResult[A]] =
    jsValueContentRead.map(_.validate[A])
}
