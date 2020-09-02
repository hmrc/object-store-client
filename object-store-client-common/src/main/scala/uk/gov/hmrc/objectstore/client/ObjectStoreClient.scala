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
import uk.gov.hmrc.objectstore.client.model.Monad
import uk.gov.hmrc.objectstore.client.model.http.{HttpClient, ObjectStoreContentRead, ObjectStoreContentWrite, ObjectStoreRead}
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}

import scala.language.higherKinds

class ObjectStoreClient[F[_], REQ_BODY, RES, RES_BODY](
  client: HttpClient[F, REQ_BODY, RES],
  read  : ObjectStoreRead[F, RES, RES_BODY],
  config: ObjectStoreClientConfig
)(implicit
  F: Monad[F]
 ) {

  private val authorizationHeader: (String, String) =
    ("Authorization", config.authorizationToken)

  private val url = s"${config.baseUrl}/object-store"

  def putObject[CONTENT](
    location: String,
    content : CONTENT
  )(implicit
    w: ObjectStoreContentWrite[F, CONTENT, REQ_BODY]
  ): F[Unit] =
    F.flatMap(w.writeContent(content))(c =>
      F.flatMap(client.put(s"$url/object/$location", c, List(authorizationHeader)))(
        read.consume
      )
    )

  def getObject[CONTENT](
    location: String
  )(implicit
    cr: ObjectStoreContentRead[F, RES_BODY, CONTENT]
  ): F[Option[Object[CONTENT]]] =
    F.flatMap(client.get(s"$url/object/$location", List(authorizationHeader)))(res =>
      F.flatMap(read.toObject(location, res)){
        case Some(obj) => F.map(cr.readContent(obj.content))(c => Some(obj.copy(content = c)))
        case None      => F.pure(None)
      }
    )

  def deleteObject(location: String): F[Unit] =
    F.flatMap(client.delete(s"$url/object/$location", List(authorizationHeader)))(read.consume)

  def listObjects(location: String): F[ObjectListing] =
    F.flatMap(client.get(s"$url/list/$location", List(authorizationHeader)))(read.toObjectListing)
}
