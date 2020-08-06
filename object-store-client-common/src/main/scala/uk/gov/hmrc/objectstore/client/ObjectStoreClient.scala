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
import uk.gov.hmrc.objectstore.client.model.http.{HttpClient, ObjectStoreRead, ObjectStoreContentWrite}
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}

import scala.language.higherKinds

class ObjectStoreClient[REQ, RES](client: HttpClient[REQ, RES], config: ObjectStoreClientConfig) {

  private val authorizationHeader: (String, String) =
    ("Authorization", config.authorizationToken)

  private val url = s"${config.baseUrl}/object-store"

  // implementation with curried types
  def putObject[F[_]]: ObjectStoreClient.PutObject[REQ, RES, F] =
    new ObjectStoreClient.PutObject(client, url, List(authorizationHeader))

  def getObject[F[_]](location: String)(implicit r : ObjectStoreRead[RES, F]): F[Option[Object[RES]]] =
    r.toObject(client.get(s"$url/object/$location", List(authorizationHeader)))

  def deleteObject[F[_]](location: String)(implicit r: ObjectStoreRead[RES, F]): F[Unit] =
    r.consume(client.delete(s"$url/object/$location", List(authorizationHeader)))

  def listObjects[F[_]](location: String)(implicit r: ObjectStoreRead[RES, F]): F[ObjectListing] =
    r.toObjectListing(client.get(s"$url/list/$location", List(authorizationHeader)))
}

object ObjectStoreClient {
  private[client] final class PutObject[REQ, RES, F[_]](
    client: HttpClient[REQ, RES],
    url    : String,
    headers: List[(String, String)]
  ) {
    def apply[CONTENT](
      location: String,
      content : CONTENT
    )(implicit
      r: ObjectStoreRead[RES, F],
      w: ObjectStoreContentWrite[CONTENT, REQ]
    ): F[Unit] =
      r.consume(client.put(s"$url/object/$location", w.writeContent(content), headers))
  }
}
