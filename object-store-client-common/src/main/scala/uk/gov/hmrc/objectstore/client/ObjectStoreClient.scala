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
  client: HttpClient[REQ_BODY, RES],
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
    F.flatMap(w.writeContent(content))(
      c => read.consume(client.put(s"$url/object/$location", c, List(authorizationHeader)))
    )

  def getObject[CONTENT](
    location: String
  )(implicit
    cr: ObjectStoreContentRead[F, RES_BODY, CONTENT]
  ): F[Option[Object[CONTENT]]] =
    cr.readContent(read.toObject(location, client.get(s"$url/object/$location", List(authorizationHeader))))

  def deleteObject(location: String): F[Unit] =
    read.consume(client.delete(s"$url/object/$location", List(authorizationHeader)))

  def listObjects(location: String): F[ObjectListing] =
    read.toObjectListing(client.get(s"$url/list/$location", List(authorizationHeader)))
}
