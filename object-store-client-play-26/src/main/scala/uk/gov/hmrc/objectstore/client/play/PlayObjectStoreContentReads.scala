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

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait PlayObjectStoreContentReads {
  implicit def akkaSourceContentRead(implicit ec: ExecutionContext): ObjectStoreContentRead[F, WSResponse, Source[ByteString, NotUsed]] =
    new ObjectStoreContentRead[F, WSResponse, Source[ByteString, NotUsed]]{
      override def readContent(response: WSResponse): F[Source[ByteString, NotUsed]] =
        F.pure(response.bodyAsSource.mapMaterializedValue(_ => NotUsed))
    }
}

trait InMemoryPlayObjectStoreContentReads extends PlayObjectStoreContentReads {

  implicit def stringContentRead(implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[F, WSResponse, String] =
    akkaSourceContentRead.mapF(src => F.liftFuture(src.map(_.utf8String).runReduce(_ + _)))

  implicit def jsValueContentRead(implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[F, WSResponse, JsValue] =
    stringContentRead.mapF(s =>
      Try(Json.parse(s)) match {
        case Failure(e) => F.raiseError(OtherError(s"Failed to parse Json: ${e.getMessage}"))
        case Success(a) => F.pure(a)
      }
    )

  implicit def jsResultContentRead[A : Reads](implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[F, WSResponse, JsResult[A]] =
    jsValueContentRead.map(_.validate[A])

  implicit def jsReadsRead[A : Reads](implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[F, WSResponse, A] =
    jsValueContentRead.map(_.as[A])
}
