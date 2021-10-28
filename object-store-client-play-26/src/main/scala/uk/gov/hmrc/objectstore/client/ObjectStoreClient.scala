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
import uk.gov.hmrc.objectstore.client.http.{HttpClient, ObjectStoreContentRead, ObjectStoreContentWrite, ObjectStoreRead, ObjectStoreWrite}

import scala.language.higherKinds
import com.typesafe.config.ConfigFactory

class ObjectStoreClient[F[_], REQ_BODY, RES, RES_BODY](
  client: HttpClient[F, REQ_BODY, RES],
  read: ObjectStoreRead[F, RES, RES_BODY],
  write: ObjectStoreWrite[F, REQ_BODY],
  config: ObjectStoreClientConfig
)(implicit F: Monad[F]) {

  private def retentionPeriodHeader(retentionPeriod: RetentionPeriod): (String, String) =
    "X-Retention-Period" -> retentionPeriod.value

  private val url = s"${config.baseUrl}/object-store"

  /**
   * Put object
   * @tparam CONTENT @see [[https://github.com/hmrc/object-store-client#put-object]]
   * @param path Path of the object in object-store under [[owner]]
   * @param content Content to upload
   * @param retentionPeriod Retention period of the object in object-store
   * @param contentType Optional Content-Type
   * @param owner Owner service of this object
   * @return [[Unit]] wrapped in the effect [[F]]
   *
   * @note Storing an object on an existing path will overwrite the previously stored object on that path.
   * */
  def putObject[CONTENT](
    path: Path.File,
    content: CONTENT,
    retentionPeriod: RetentionPeriod = config.defaultRetentionPeriod,
    contentType: Option[String] = None,
    contentMd5: Option[Md5Hash] = None,
    owner: String = config.owner
  )(implicit w: ObjectStoreContentWrite[F, CONTENT, REQ_BODY], hc: HeaderCarrier): F[ObjectSummary] =
    F.flatMap(w.writeContent(content, contentType, contentMd5))(c =>
      F.flatMap(client.put(s"$url/object/$owner/${path.asUri}", c, headers(retentionPeriodHeader(retentionPeriod))))(
        read.toObjectSummary
      )
    )

  /**
   * Get object
   * @tparam CONTENT @see [[https://github.com/hmrc/object-store-client#get-object]]
   * @param path Path of the object in object-store under [[owner]]
   * @param owner Owner service of this object
   * @return optional [[Object]]`[`[[CONTENT]]`]` wrapped in the effect [[F]]
   * */
  def getObject[CONTENT](
    path: Path.File,
    owner: String = config.owner
  )(implicit cr: ObjectStoreContentRead[F, RES_BODY, CONTENT], hc: HeaderCarrier): F[Option[Object[CONTENT]]] = {
    F.flatMap(client.get(s"$url/object/$owner/${path.asUri}", headers()))(res =>
      F.flatMap(read.toObject(s"$owner/${path.asUri}", res)) {
        case Some(obj) => F.map(cr.readContent(obj.content))(c => Some(obj.copy(content = c)))
        case None      => F.pure(None)
      }
    )
  }

  /**
   * Delete object
   * @param path Path of the object in object-store under owner
   * @param owner Owner service of this object
   * @return [[Unit]] wrapped in the effect [[F]]
   * */
  def deleteObject(
    path: Path.File,
    owner: String = config.owner
  )(implicit hc: HeaderCarrier): F[Unit] =
    F.flatMap(client.delete(s"$url/object/$owner/${path.asUri}", headers()))(read.consume)

  /**
   * List objects
   * @param path Path of the object in object-store under owner
   * @param owner Owner service
   * @return [[ObjectListing]] wrapped in the effect [[F]]
   * */
  def listObjects(
    path: Path.Directory,
    owner: String = config.owner
  )(implicit hc: HeaderCarrier): F[ObjectListings] = {
    val location = s"$url/list/$owner/${path.asUri}".stripSuffix("/") // strip suffix since you can list an empty path
    F.flatMap(client.get(location, headers()))(read.toObjectListings)
  }

  def zip(
    from           : Path.Directory,
    to             : Path.File,
    retentionPeriod: RetentionPeriod = config.defaultRetentionPeriod,
    fromOwner      : String          = config.owner,
    toOwner        : String          = config.owner
  )(implicit hc: HeaderCarrier): F[ObjectSummary] =
    F.flatMap(
      write.writeZipRequest(
        ZipRequest(
          from            = Path.Directory(s"object-store/object/$fromOwner/${if (from.asUri == "/") "" else from.asUri}"),
          to              = Path.File(s"object-store/object/$toOwner/${to.asUri}"),
          retentionPeriod = retentionPeriod
        )
      )
    )(reqBody =>
      F.flatMap(
        client.post(
          s"$url/ops/zip",
          reqBody,
          headers()
        )
      )(read.toObjectSummary)
    )

  private val hcConfig = HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  private def headers(additionalHeaders: (String, String)*)(implicit hc: HeaderCarrier): List[(String, String)] =
    hc
      .copy(authorization = Some(Authorization(config.authorizationToken)))
      .headersForUrl(hcConfig)(url)
      .toList ++
        additionalHeaders

}
