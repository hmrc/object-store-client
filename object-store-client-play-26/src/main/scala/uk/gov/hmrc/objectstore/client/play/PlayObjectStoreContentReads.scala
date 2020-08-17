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

import scala.concurrent.{ExecutionContext, Future}

trait PlayObjectStoreContentReads {

  implicit def akkaSourceContentRead: ObjectStoreContentRead[Future, WSResponse, Source[ByteString, NotUsed]] =
    new ObjectStoreContentRead[Future, WSResponse, Source[ByteString, NotUsed]]{
      override def readContent(response: WSResponse): Future[Source[ByteString, NotUsed]] =
        Future.successful(response.bodyAsSource.mapMaterializedValue(_ => NotUsed))
    }
}

trait InMemoryPlayObjectStoreContentReads extends PlayObjectStoreContentReads {
  implicit def stringContentRead(implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[Future, WSResponse, String] =
    akkaSourceContentRead.mapF(_.map(_.utf8String).runReduce(_ + _))

  // or we can just use a blocking implementation, avoiding the requirement for Materializer?
  /*implicit val stringContentRead: ObjectStoreContentRead[Future, WSResponse, String] =
    new ObjectStoreContentRead[Future, WSResponse, String]{
      override def readContent(response: WSResponse): Future[String] =
        Future.successful(response.body)
    }*/

  implicit def jsValueContentRead(implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[Future, WSResponse, JsValue] =
    stringContentRead.map(Json.parse)

  implicit def jsResultContentRead[A : Reads](implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[Future, WSResponse, JsResult[A]] =
    jsValueContentRead.map(_.validate[A])

  implicit def jsReadsRead[A : Reads](implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentRead[Future, WSResponse, A] =
    jsValueContentRead.map(_.as[A])
}

object PlayObjectStoreContentReads extends PlayObjectStoreContentReads

case class UpstreamErrorResponse(message: String, statusCode: Int) extends Exception(message)
