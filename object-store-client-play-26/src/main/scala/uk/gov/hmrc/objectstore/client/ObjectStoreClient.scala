/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.objectstore.client.category.Monad
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.http.{HttpClient, ObjectStoreContentRead, ObjectStoreContentWrite, ObjectStoreRead}

import scala.language.higherKinds
import com.typesafe.config.ConfigFactory

class ObjectStoreClient[F[_], REQ_BODY, RES, RES_BODY](
  client: HttpClient[F, REQ_BODY, RES],
  read: ObjectStoreRead[F, RES, RES_BODY],
  config: ObjectStoreClientConfig
)(implicit F: Monad[F]) {

  private def retentionPeriodHeader(retentionPeriod: RetentionPeriod): (String, String) =
    "X-Retention-Period" -> retentionPeriod.value

  private val url = s"${config.baseUrl}/object-store"

  /** Storing an object on an existing path will overwrite the previously stored object on that path. */
  def putObject[CONTENT](
    path: Path.File,
    content: CONTENT,
    retentionPeriod: RetentionPeriod = config.defaultRetentionPeriod,
    contentType: Option[String] = None,
    owner: String = config.owner
  )(implicit w: ObjectStoreContentWrite[F, CONTENT, REQ_BODY], hc: HeaderCarrier): F[Unit] =
    F.flatMap(w.writeContent(content, contentType))(c =>
      F.flatMap(client.put(s"$url/object/$owner/${path.asUri}", c, headers(retentionPeriodHeader(retentionPeriod))))(
        read.consume
      )
    )

  def getObject[CONTENT](
    path: Path.File,
    owner: String = config.owner
  )(implicit cr: ObjectStoreContentRead[F, RES_BODY, CONTENT], hc: HeaderCarrier): F[Option[Object[CONTENT]]] = {
    val location = s"$url/object/$owner/${path.asUri}"
    F.flatMap(client.get(location, headers()))(res =>
      F.flatMap(read.toObject(location, res)) {
        case Some(obj) => F.map(cr.readContent(obj.content))(c => Some(obj.copy(content = c)))
        case None      => F.pure(None)
      }
    )
  }

  def deleteObject(
    path: Path.File,
    owner: String = config.owner
  )(implicit hc: HeaderCarrier): F[Unit] =
    F.flatMap(client.delete(s"$url/object/$owner/${path.asUri}", headers()))(read.consume)

  def listObjects(
    path: Path.Directory,
    owner: String = config.owner
  )(implicit hc: HeaderCarrier): F[ObjectListing] = {
    val location = s"$url/list/$owner/${path.asUri}".stripSuffix("/") // strip suffix since you can list an empty path
    F.flatMap(client.get(location, headers()))(read.toObjectListing)
  }

  private val hcConfig = HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  private def headers(additionalHeaders: (String, String)*)(implicit hc: HeaderCarrier): List[(String, String)] =
    hc
      .copy(authorization = Some(Authorization(config.authorizationToken)))
      .headersForUrl(hcConfig)(url)
      .toList ++
        additionalHeaders

}
