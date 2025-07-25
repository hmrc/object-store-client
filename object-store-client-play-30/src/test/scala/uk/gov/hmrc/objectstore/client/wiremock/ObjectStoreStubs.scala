/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.objectstore.client.wiremock

import java.net.URL

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.objectstore.client.play.Md5HashUtils
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectSummaryWithMd5, Path, PresignedDownloadUrl, RetentionPeriod, Sha256Checksum}

object ObjectStoreStubs {

  def initPutObjectStub(
    path           : Path.File,
    reqBody        : Array[Byte],
    md5Base64      : Md5Hash,
    owner          : String,
    retentionPeriod: RetentionPeriod = RetentionPeriod.OneWeek,
    contentType    : String          = "application/octet-stream",
    statusCode     : Int,
    response       : Option[ObjectSummaryWithMd5]
  ): Unit = {
    val request = put(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization"     , equalTo("AuthorizationToken"))
      .withHeader("Content-Length"    , equalTo("49"))
      .withHeader("Content-Type"      , equalTo(contentType))
      .withHeader("Content-MD5"       , equalTo(md5Base64.value))
      .withHeader("X-Retention-Period", equalTo(retentionPeriod.value))
      .withRequestBody(binaryEqualTo(reqBody))

    val responseBuilder =
      response.foldLeft(aResponse.withStatus(statusCode)){ case (builder, response) =>
        builder
          .withHeader("Content-Type", "application/json")
          .withBody(
            Json.obj(
              "location"      -> response.location.asUri,
              "contentLength" -> response.contentLength,
              "contentMD5"    -> response.contentMd5.value,
              "lastModified"  -> response.lastModified
            ).toString
          )
      }

    stubFor(
      request
        .willReturn(responseBuilder)
    )
  }

  def initListObjectsStub(
    path       : Path.Directory,
    statusCode : Int,
    resBodyJson: Option[String],
    owner      : String
  ): Unit = {
    val request = get(urlEqualTo(s"/object-store/list/$owner/${path.asUri}".stripSuffix("/")))
      .withHeader("Authorization", equalTo("AuthorizationToken"))

    val responseBuilder = aResponse().withStatus(statusCode)
    resBodyJson.foreach { body =>
      responseBuilder.withBody(body)
      responseBuilder.withHeader("Content-Type", "application/json")
    }

    stubFor(
      request
        .willReturn(responseBuilder)
    )
  }

  def initDeleteObjectStub(
    path      : Path.File,
    statusCode: Int       = 200,
    owner     : String
  ): Unit = {
    val request = delete(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))
    val response = aResponse()
      .withStatus(statusCode)

    stubFor(
      request
        .willReturn(response)
    )
  }

  def initGetObjectStub(path: Path.File, statusCode: Int, resBody: Option[String], owner: String): Unit = {
    val request = get(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))

    val responseBuilder = aResponse.withStatus(statusCode)
    resBody.foreach { body =>
      responseBuilder
        .withBody(body)
        .withHeader("Content-Length", body.getBytes.length.toString)
        .withHeader("Content-Type"  , "application/octet-stream")
        .withHeader("Content-MD5"   , Md5HashUtils.fromBytes(body.getBytes).value)
        .withHeader("Last-Modified" , "Tue, 18 Aug 2020 10:15:30 GMT")
        .withHeader("Location"      , s"/object-store/object/${path.asUri}")
    }

    stubFor(
      request
        .willReturn(responseBuilder)
    )
  }

  def initZipStub(
    from           : Path.Directory,
    to             : Path.File,
    retentionPeriod: RetentionPeriod,
    fromOwner      : String,
    toOwner        : String,
    statusCode     : Int,
    response       : Option[ObjectSummaryWithMd5]
  ): Unit = {
    val request =
      post(urlEqualTo("/object-store/ops/zip"))
        .withHeader("Authorization", equalTo("AuthorizationToken"))
        .withHeader("Content-Type" , equalTo("application/json"))
        .withRequestBody(equalToJson(s"""{
          "fromLocation": "object-store/object/$fromOwner/${from.asUri}",
          "toLocation": "object-store/object/$toOwner/${to.asUri}",
          "retentionPeriod": "${retentionPeriod.value}"
        }"""))

    val responseBuilder =
      response.foldLeft(aResponse.withStatus(statusCode)){ case (builder, response) =>
        builder
          .withHeader("Content-Type", "application/json")
          .withBody(
            Json.obj(
              "location"      -> response.location.asUri,
              "contentLength" -> response.contentLength,
              "contentMD5"    -> response.contentMd5.value,
              "lastModified"  -> response.lastModified
            ).toString
          )
      }

    stubFor(
      request
        .willReturn(responseBuilder)
    )
  }

  def initUploadFromUrlStub(
    from           : URL,
    to             : Path.File,
    retentionPeriod: RetentionPeriod,
    contentType    : Option[String],
    contentMd5     : Option[Md5Hash],
    contentSha256  : Option[Sha256Checksum],
    owner          : String,
    statusCode     : Int,
    response       : Option[ObjectSummaryWithMd5]
  ): Unit = {

    val request =
      post(urlEqualTo("/object-store/ops/upload-from-url"))
        .withHeader("Authorization", equalTo("AuthorizationToken"))
        .withHeader("Content-Type" , equalTo("application/json"))
        .withRequestBody(equalToJson(s"""{
          "fromUrl": "${from.toString}",
          "toLocation": "object-store/object/$owner/${to.asUri}",
          "retentionPeriod": "${retentionPeriod.value}"${contentType.fold(""){contentType => s""",
          "contentType": "$contentType""""}}${contentMd5.fold(""){contentMd5 => s""",
          "contentMD5": "${contentMd5.value}""""}}${contentSha256.fold(""){contentSha256 => s""",
          "contentSHA256": "${contentSha256.value}""""}}
        }"""))

    val responseBuilder =
      response.foldLeft(aResponse.withStatus(statusCode)){ case (builder, response) =>
        builder
          .withHeader("Content-Type", "application/json")
          .withBody(
            Json.obj(
              "location"      -> response.location.asUri,
              "contentLength" -> response.contentLength,
              "contentMD5"    -> response.contentMd5.value,
              "lastModified"  -> response.lastModified
            ).toString
          )
      }

    stubFor(
      request
        .willReturn(responseBuilder)
    )
  }

  def initPresignedDownloadUrlStub(
    path      : Path.File,
    owner     : String,
    statusCode: Int = 200,
    response  : Option[PresignedDownloadUrl]
  ): Unit = {
    val request =
      post(urlEqualTo("/object-store/ops/presigned-url"))
        .withHeader("Authorization", equalTo("AuthorizationToken"))
        .withHeader("Content-Type" , equalTo("application/json"))
        .withRequestBody(equalToJson(s"""{
          "location": "object-store/object/$owner/${path.asUri}"
        }"""))

    val responseBuilder =
      response.foldLeft(aResponse.withStatus(statusCode)){ case (builder, response) =>
        builder
          .withHeader("Content-Type", "application/json")
          .withBody(
            Json.obj(
              "downloadUrl"   -> response.downloadUrl.toString,
              "contentLength" -> response.contentLength,
              "contentMD5"    -> response.contentMd5.value
            ).toString
          )
      }

    stubFor(
      request
        .willReturn(responseBuilder)
    )
  }
}
