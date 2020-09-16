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

package uk.gov.hmrc.objectstore.client.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, binaryEqualTo, delete, equalTo, get, put, stubFor, urlEqualTo}
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.Md5Hash

object ObjectStoreStubs {

  def initPutObjectStub(
    path: Path.File,
    statusCode: Int,
    reqBody: Array[Byte],
    md5Base64: String,
    contentType: String = "application/octet-stream",
    owner: String
  ): Unit = {
    val request = put(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))
      .withHeader("Content-Length", equalTo("49"))
      .withHeader("Content-Type", equalTo(contentType))
      .withHeader("Content-MD5", equalTo(md5Base64))
      .withRequestBody(binaryEqualTo(reqBody))

    val response = aResponse().withStatus(statusCode)
    stubFor(
      request
        .willReturn(response))
  }

  def initListObjectsStub(
    path: String,
    statusCode: Int,
    resBodyJson: Option[String],
    owner: String
  ): Unit = {
    val request = get(urlEqualTo(s"/object-store/list/$owner/$path"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))

    val responseBuilder = aResponse().withStatus(statusCode)
    resBodyJson.foreach { body =>
      responseBuilder.withBody(body)
      responseBuilder.withHeader("Content-Type", "application/json")
    }

    stubFor(
      request
        .willReturn(responseBuilder))
  }

  def initDeleteObjectStub(
    path: Path.File,
    statusCode: Int = 200,
    owner: String
  ): Unit = {
    val request = delete(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))
    val response = aResponse()
      .withStatus(statusCode)

    stubFor(
      request
        .willReturn(response))
  }

  def initGetObjectStub(path: Path.File, statusCode: Int, resBody: Option[String], owner: String): Unit = {
    val request = get(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))

    val responseBuilder = aResponse.withStatus(statusCode)
    resBody.foreach { body =>
      responseBuilder
        .withBody(body)
        .withHeader("Content-Length", body.getBytes.length.toString)
        .withHeader("Content-Type", "application/octet-stream")
        .withHeader("Content-MD5", Md5Hash.fromBytes(body.getBytes))
        .withHeader("Last-Modified", "Tue, 18 Aug 2020 10:15:30 GMT")
        .withHeader("Location", s"/object-store/object/${path.asUri}")
    }

    stubFor(
      request
        .willReturn(responseBuilder))
  }


}
