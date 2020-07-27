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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreRead
import uk.gov.hmrc.objectstore.client.model.objectstore
import uk.gov.hmrc.objectstore.client.model.objectstore.ObjectListing

import scala.concurrent.{ExecutionContext, Future}

object ObjectStoreReads {

   class PlayFutureObjectStoreRead(implicit ec: ExecutionContext) extends ObjectStoreRead[Future[WSResponse], Source[ByteString, _], Future]{

    override def toObjectListing(response: Future[WSResponse]): Future[ObjectListing] = {
      response.map(_.body[JsValue].as[ObjectListing](PlayFormats.objectListingFormat))
    }

    override def toObject(response: Future[WSResponse]): Future[Option[objectstore.Object[Source[ByteString, _]]]] = {
      response.map {
        case r if Status.isSuccessful(r.status) => Some(objectstore.Object("", r.bodyAsSource))
        case r if Status.isClientError(r.status) => None
      }
    }

    override def consume(response: Future[WSResponse]): Future[Unit] = {
      response.map(_ => ())
    }
  }
}
