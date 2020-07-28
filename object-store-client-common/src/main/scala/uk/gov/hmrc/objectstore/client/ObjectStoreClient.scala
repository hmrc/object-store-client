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

package uk.gov.hmrc.objectstore.client

import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreReadSyntax._
import uk.gov.hmrc.objectstore.client.model.http.{HttpClient, ObjectStoreRead, ObjectStoreWrite}
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}

import scala.language.higherKinds

class ObjectStoreClient[HttpResponse](client: HttpClient[HttpResponse], config: ObjectStoreClientConfig) {

  private val url = s"${config.baseUrl}/object-store"

  // implementation with curried types
  def putObject[F[_]]: ObjectStoreClient.PutObject[F, HttpResponse] =
    new ObjectStoreClient.PutObject(client, url)

  def getObject[T, F[_]](location: String)(implicit rt: ObjectStoreRead[HttpResponse, T, F]): F[Option[Object[T]]] =
    client.get(s"$url/object/$location").toObject

  def deleteObject[F[_]](location: String)(implicit rt: ObjectStoreRead[HttpResponse, _, F]): F[Unit] =
    client.delete(s"$url/object/$location").consume

  def listObjects[F[_]](location: String)(implicit rt: ObjectStoreRead[HttpResponse, _, F]): F[ObjectListing] =
    client.get(s"$url/list/$location").toObjectListings
}

object ObjectStoreClient {
  private[client] final class PutObject[F[_], HttpResponse](client: HttpClient[HttpResponse], url: String) {
    def apply[BODY : ObjectStoreWrite](location: String, content: BODY)(implicit rt: ObjectStoreRead[HttpResponse, _, F]): F[Unit] =
      client.put(s"$url/object/$location", content).consume
  }
}
