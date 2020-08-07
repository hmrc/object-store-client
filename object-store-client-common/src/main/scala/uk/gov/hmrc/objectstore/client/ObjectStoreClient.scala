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
import uk.gov.hmrc.objectstore.client.model.http.{HttpClient, ObjectStoreRead, ObjectStoreContentRead, ObjectStoreContentWrite}
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}
import uk.gov.hmrc.objectstore.client.model.Monad

import scala.language.higherKinds

class ObjectStoreClient[F[_], BODY, RES](client: HttpClient[F, BODY, RES], config: ObjectStoreClientConfig)(implicit F: Monad[F]) {

  private val authorizationHeader: (String, String) =
    ("Authorization", config.authorizationToken)

  private val url = s"${config.baseUrl}/object-store"

  // implementation with curried types
  def putObject: ObjectStoreClient.PutObject[F, BODY, RES] =
    new ObjectStoreClient.PutObject(client, url, List(authorizationHeader))

  def getObject[CONTENT](
    location: String
  )(implicit
    r: ObjectStoreRead[F, RES],
    cr: ObjectStoreContentRead[F, RES, CONTENT]
  ): F[Option[Object[CONTENT]]] =
    F.flatMap(client.get(s"$url/object/$location", List(authorizationHeader)))(res => r.toObject(res, cr.readContent))

  def deleteObject(location: String)(implicit r: ObjectStoreRead[F, RES]): F[Unit] =
    F.flatMap(client.delete(s"$url/object/$location", List(authorizationHeader)))(r.consume)

  def listObjects(location: String)(implicit r: ObjectStoreRead[F, RES]): F[ObjectListing] =
    F.flatMap(client.get(s"$url/list/$location", List(authorizationHeader)))(r.toObjectListing)
}

object ObjectStoreClient {
  private[client] final class PutObject[F[_], BODY, RES](
    client : HttpClient[F, BODY, RES],
    url    : String,
    headers: List[(String, String)]
  )(implicit F: Monad[F]
  ) {
    def apply[CONTENT](
      location: String,
      content : CONTENT
    )(implicit
      r: ObjectStoreRead[F, RES],
      w: ObjectStoreContentWrite[F, CONTENT, BODY]
    ): F[Unit] =
      F.flatMap(w.writeContent(content))(c =>
        F.flatMap(client.put(s"$url/object/$location", c, headers))(
          r.consume
        )
      )
  }
}
