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
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreWriteSyntax._
import uk.gov.hmrc.objectstore.client.model.http.{HttpClient, ObjectStoreRead, ObjectStoreWrite}
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}

import scala.language.higherKinds

class ObjectStoreClient[REQ, RES](client: HttpClient[REQ, RES], config: ObjectStoreClientConfig) {

  private val url = s"${config.baseUrl}/object-store"

  // implementation with curried types
  def putObject[F[_]]: ObjectStoreClient.PutObject[REQ, RES, F] =
    new ObjectStoreClient.PutObject(client, url)

  // implementation with curried types
  def getObject[F[_]]: ObjectStoreClient.GetObject[REQ, RES, F] =
    new ObjectStoreClient.GetObject(client, url)

  def deleteObject[F[_]](location: String)(implicit rt: ObjectStoreRead[RES, _, F]): F[Unit] =
    client.delete(s"$url/object/$location").consume

  def listObjects[F[_]](location: String)(implicit rt: ObjectStoreRead[RES, _, F]): F[ObjectListing] =
    client.get(s"$url/list/$location").toObjectListings
}

object ObjectStoreClient {
  private[client] final class GetObject[REQ, RES, F[_]](client: HttpClient[REQ, RES], url: String) {
    def apply[T](location: String)(implicit rt: ObjectStoreRead[RES, T, F]): F[Option[Object[T]]] =
      client.get(s"$url/object/$location").toObject
  }

  private[client] final class PutObject[REQ, RES, F[_]](client: HttpClient[REQ, RES], url: String) {
    def apply[BODY](location: String, content: BODY)(
      implicit rt: ObjectStoreRead[RES, _, F],
      wt: ObjectStoreWrite[BODY, REQ]): F[Unit] =
      client.put(s"$url/object/$location", content.write).consume
  }
}
