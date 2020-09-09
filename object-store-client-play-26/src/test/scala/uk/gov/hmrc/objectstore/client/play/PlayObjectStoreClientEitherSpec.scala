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

package uk.gov.hmrc.objectstore.client.play

import java.time.Instant
import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, EitherValues, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.objectstore.client.{ObjectListing, ObjectSummary, Path}
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.http.Payload

import scala.concurrent.ExecutionContextExecutor

class PlayObjectStoreClientEitherSpec
    extends WordSpec
    with Matchers
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with EitherValues {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val system: ActorSystem          = ActorSystem()
  implicit val m: ActorMaterializer         = ActorMaterializer()

  protected val osClient: PlayObjectStoreClientEither = fakeApplication().injector.instanceOf(classOf[PlayObjectStoreClientEither])

  lazy val owner = "my-service"

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(bind[ObjectStoreClientConfig].toInstance(ObjectStoreClientConfig(
          baseUrl            = wireMockUrl,
          owner              = owner,
          authorizationToken = "AuthorizationToken"
        )))
      .build()

  import Implicits._

  "putObject" must {

    "store an object as Source with NotUsed bound to Mat" in {
      val body                                = s"hello world! ${UUID.randomUUID().toString}"
      val path                                = generateFilePath()
      val md5Base64                           = Md5Hash.fromBytes(body.getBytes)
      val source: Source[ByteString, NotUsed] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64)

      osClient.putObject(path, source).futureValue.right.value shouldBe (())
    }

    "store an object as Source with Any bound to Mat" in {
      val body                          = s"hello world! ${UUID.randomUUID().toString}"
      val path                          = generateFilePath()
      val md5Base64                     = Md5Hash.fromBytes(body.getBytes)
      val source: Source[ByteString, _] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64)

      osClient.putObject(path, source).futureValue.right.value shouldBe (())
    }

    "store an object as Source with NotUsed bound to Mat and known md5hash and length" in {
      val body                                = s"hello world! ${UUID.randomUUID().toString}"
      val path                                = generateFilePath()
      val md5Base64                           = Md5Hash.fromBytes(body.getBytes)
      val source: Source[ByteString, NotUsed] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64)

      osClient.putObject(path, Payload(length = body.length, md5Hash = md5Base64, content = source)).futureValue.right.value shouldBe (())
    }

    "store an object as Source with Any bound to Mat and known md5hash and length" in {
      val body                          = s"hello world! ${UUID.randomUUID().toString}"
      val path                          = generateFilePath()
      val md5Base64                     = Md5Hash.fromBytes(body.getBytes)
      val source: Source[ByteString, _] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64)

      osClient.putObject(path, Payload(length = body.length, md5Hash = md5Base64, content = source)).futureValue.right.value shouldBe (())
    }

    "store an object as Bytes" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}".getBytes
      val path      = generateFilePath()
      val md5Base64 = Md5Hash.fromBytes(body)

      initPutObjectStub(path, statusCode = 201, body, md5Base64)

      osClient.putObject(path, body).futureValue.right.value shouldBe (())
    }

    "store an object as String" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}"
      val path      = generateFilePath()
      val md5Base64 = Md5Hash.fromBytes(body.getBytes)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64)

      osClient.putObject(path, body).futureValue.right.value shouldBe (())
    }

    "return an exception if object-store response is not successful" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}"
      val path      = generateFilePath()
      val md5Base64 = Md5Hash.fromBytes(body.getBytes)

      initPutObjectStub(path, statusCode = 401, body.getBytes, md5Base64)

      osClient.putObject(path, toSource(body)).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "getObject" must {
    "return an object that exists" in {
      val body     = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body))

      val obj = osClient.getObject[Source[ByteString, NotUsed]](path).futureValue.right.value
      obj.get.content.asString() shouldBe body
    }

    "return an object that exists as String" in {
      val body     = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body))

      import InMemoryReads.stringContentRead

      val obj = osClient.getObject[String](path).futureValue.right.value
      obj.get.content shouldBe body
    }

    case class Obj(k1: String, k2: String)
    implicit val or: Reads[Obj] =
      ( (__ \ "k1").read[String]
      ~ (__ \ "k2").read[String]
      )(Obj.apply _)

    "return an object that exists as JsValue" in {
      val body     = """{ "k1": "v1", "k2": "v2" }"""
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body))

      import InMemoryReads.jsValueContentRead

      val obj = osClient.getObject[JsValue](path).futureValue.right.value
      obj.get.content shouldBe JsObject(Seq("k1" -> JsString("v1"), "k2" -> JsString("v2")))
    }

    "fail with invalid json when reading as JsValue" in {
      val body     = """{ "k1": "v1", "k2": "v2""""
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body))

      import InMemoryReads.jsValueContentRead

      osClient.getObject[JsValue](path).futureValue.left.value shouldBe an[PlayObjectStoreException]
    }

    "return an object that exists as JsResult" in {
      val body     = """{ "k1": "v1", "k2": "v2" }"""
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body))

      import InMemoryReads._

      val obj = osClient.getObject[JsResult[Obj]](path).futureValue.right.value
      obj.get.content shouldBe JsSuccess(Obj(k1 = "v1", k2 = "v2"), __)
    }

    "return an object that exists as JsReads" in {
      val body     = """{ "k1": "v1", "k2": "v2" }"""
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body))

      import InMemoryReads._

      val obj = osClient.getObject[Obj](path).futureValue.right.value
      obj.get.content shouldBe Obj(k1 = "v1", k2 = "v2")
    }

    "return None for an object that doesn't exist" in {
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 404, None)

      osClient.getObject(path).futureValue.right.value shouldBe None
    }

    "return an exception if object-store response is not successful" in {
      val path     = generateFilePath()

      initGetObjectStub(path, statusCode = 401, None)

      osClient.getObject(path).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "deleteObject" must {
    "delete an object" in {
      val path     = generateFilePath()

      initDeleteObjectStub(path)

      osClient.deleteObject(path).futureValue.right.value shouldBe (())
    }

    "return an exception if object-store response is not successful" in {
      val path     = generateFilePath()

      initDeleteObjectStub(path, statusCode = 401)

      osClient.deleteObject(path).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "listObject" must {
    "return a ObjectListing with objectSummaries" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 200, Some(objectListingJson))

      osClient.listObjects(path).futureValue.right.value.objectSummaries shouldBe List(
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "0993180f-8f31-41b2-905c-71f0273bb7d4"),
          contentLength = 49,
          contentMd5    = "4033ff85a6fdc6a2f51e60d89236a244",
          lastModified  = Instant.parse("2020-07-21T13:16:42.859Z")
        ),
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "23265eab-268e-4fcc-904f-775586b362c2"),
          contentLength = 49,
          contentMd5    = "a3c2f1e38701bd2c7b54ebd7b1cd0dbc",
          lastModified  = Instant.parse("2020-07-21T13:16:41.226Z")
        )
      )
    }

    "return a ObjectListing with no objectSummaries" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 200, Some(emptyObjectListingJson))

      osClient.listObjects(path).futureValue.right.value shouldBe ObjectListing(List.empty)
    }

    "return an exception if object-store response is not successful" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 401, None)

      osClient.listObjects(path).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  override def afterAll: Unit = {
    super.afterAll
    system.terminate()
  }

  implicit class SourceOps(source: Source[ByteString, _]) {

    def asString(): String =
      source.map(_.utf8String).runReduce(_ + _).futureValue
  }

  private def generateDirectoryPath(): Path.Directory =
    Path.Directory(UUID.randomUUID().toString)

  private def generateFilePath(): Path.File =
    Path.File(generateDirectoryPath(), UUID.randomUUID().toString)

  private def toSource(body: String): Source[ByteString, NotUsed] =
    Source.single(ByteString(body.getBytes("UTF-8")))

  private def objectListingJson: String =
    """{
      |  "objects": [
      |    {
      |      "location": "/object-store/object/something/0993180f-8f31-41b2-905c-71f0273bb7d4",
      |      "contentLength": 49,
      |      "contentMD5": "4033ff85a6fdc6a2f51e60d89236a244",
      |      "lastModified": "2020-07-21T13:16:42.859Z"
      |    },
      |    {
      |      "location": "/object-store/object/something/23265eab-268e-4fcc-904f-775586b362c2",
      |      "contentLength": 49,
      |      "contentMD5": "a3c2f1e38701bd2c7b54ebd7b1cd0dbc",
      |      "lastModified": "2020-07-21T13:16:41.226Z"
      |    }
      |  ]
      |}""".stripMargin

  private def emptyObjectListingJson: String =
    """{
      |    "objects": []
      |}""".stripMargin

  private def initPutObjectStub(path: Path.File, statusCode: Int, reqBody: Array[Byte], md5Base64: String): Unit = {
    val request = put(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))
      .withHeader("Content-Length", equalTo("49"))
      .withHeader("Content-Type", equalTo("application/octet-stream"))
      .withHeader("Content-MD5", equalTo(md5Base64))
      .withRequestBody(binaryEqualTo(reqBody))

    val response = aResponse().withStatus(statusCode)
    stubFor(
      request
        .willReturn(response))
  }

  private def initGetObjectStub(path: Path.File, statusCode: Int, resBody: Option[String]): Unit = {
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

  private def initDeleteObjectStub(path: Path.File, statusCode: Int = 200): Unit = {
    val request = delete(urlEqualTo(s"/object-store/object/$owner/${path.asUri}"))
      .withHeader("Authorization", equalTo("AuthorizationToken"))
    val response = aResponse()
      .withStatus(statusCode)

    stubFor(
      request
        .willReturn(response))
  }

  private def initListObjectsStub(path: Path.Directory, statusCode: Int, resBodyJson: Option[String]): Unit = {
    val request = get(urlEqualTo(s"/object-store/list/$owner/${path.asUri}"))
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
}
