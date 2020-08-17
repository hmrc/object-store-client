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

class ObjectStoreClient[F[_], BODY, RES, X](
  client: HttpClient[BODY, RES],
  r     : ObjectStoreRead[F, RES, X],
  config: ObjectStoreClientConfig
)(implicit F: Monad[F]) {
  private val authorizationHeader: (String, String) =
    ("Authorization", config.authorizationToken)

  private val url = s"${config.baseUrl}/object-store"

  def putObject[CONTENT](
      location: String,
      content : CONTENT
    )(implicit
      w: ObjectStoreContentWrite[F, CONTENT, BODY]
    ): F[Unit] =
    F.map(w.writeContent(content)){c =>
      r.consume(client.put(s"$url/object/$location", c, List(authorizationHeader)))
    }

  def getObject[CONTENT](
    location: String
  )(implicit
    cr: ObjectStoreContentRead[F, X, CONTENT]
  ): F[Option[Object[CONTENT]]] =
    F.flatMap(r.toObject(client.get(s"$url/object/$location", List(authorizationHeader)))){
          maybeObj => maybeObj.fold[F[Option[Object[CONTENT]]]](F.pure(None)) { obj =>
            F.map(cr.readContent(obj.content))(c => Some(obj.copy(content = c)))
          }
    }

  def deleteObject(location: String): F[Unit] =
    r.consume(client.delete(s"$url/object/$location", List(authorizationHeader)))

  def listObjects(location: String): F[ObjectListing] =
    r.toObjectListing(client.get(s"$url/list/$location", List(authorizationHeader)))
}
