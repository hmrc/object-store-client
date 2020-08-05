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

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.model.objectstore.{ObjectListing, ObjectSummary}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient.Implicits._

import scala.concurrent.ExecutionContextExecutor

class PlayObjectStoreClientSpec
    extends WordSpec
    with Matchers
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val system: ActorSystem          = ActorSystem()
  implicit val m: ActorMaterializer         = ActorMaterializer()

  protected val osClient: PlayObjectStoreClient = fakeApplication().injector.instanceOf(classOf[PlayObjectStoreClient])

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(bind[ObjectStoreClientConfig].toInstance(ObjectStoreClientConfig(wireMockUrl, "AuthorizationToken")))
      .build()

  "putObject" must {

    "store an object" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}"
      val location  = generateLocation()
      val content   = generateContent(body)
      val md5Base64 = Md5Hash.fromBytes(body.getBytes)

      putObjectStub(location, statusCode = 201, body, md5Base64)

      osClient.putObject(location, content).futureValue shouldBe ((): Unit)
    }

    "return an exception if object-store response is not successful" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}"
      val location  = generateLocation()
      val content   = generateContent(body)
      val md5Base64 = Md5Hash.fromBytes(body.getBytes)

      putObjectStub(location, statusCode = 401, body, md5Base64)
      osClient.putObject(location, content).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }
  }

  "getObject" must {
    "return an object that exists" in {
      val body     = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val location = generateLocation()
      val content  = generateContent(body)

      getObjectStub(location, statusCode = 200, Some(body))

      osClient.getObject[scala.concurrent.Future, Source[ByteString, NotUsed]](location).futureValue.get.objectContent.asString() shouldBe content.asString()
    }

    "return an object that exists as String" in {
      val body     = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val location = generateLocation()
      val content  = generateContent(body)

      getObjectStub(location, statusCode = 200, Some(body))

      osClient.getObject[scala.concurrent.Future, String](location).futureValue.get.objectContent shouldBe content.asString()
    }

    "return None for an object that doesn't exist" in {
      val location = generateLocation()
      getObjectStub(location, statusCode = 404, None)
      osClient.getObject[scala.concurrent.Future, Source[ByteString, NotUsed]](location).futureValue shouldBe None
    }

    "return an exception if object-store response is not successful" in {
      val location = generateLocation()
      getObjectStub(location, statusCode = 401, None)
      osClient.getObject[scala.concurrent.Future, Source[ByteString, NotUsed]](location).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }
  }

  "deleteObject" must {
    "delete an object" in {
      val location = generateLocation()

      deleteObjectStub(location)

      osClient.deleteObject(location).futureValue shouldBe ((): Unit)
    }

    "return an exception if object-store response is not successful" in {
      val location = generateLocation()
      deleteObjectStub(location, statusCode = 401)
      osClient.deleteObject(location).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }
  }

  "listObject" must {
    "return a ObjectListing with objectSummaries" in {
      val location = generateLocation()

      listObjectsStub(location, statusCode = 200, Some(objectListingJson))

      osClient.listObjects(location).futureValue.objectSummaries shouldBe List(
        ObjectSummary(
          location      = "something/0993180f-8f31-41b2-905c-71f0273bb7d4",
          contentLength = 49,
          contentMd5    = "4033ff85a6fdc6a2f51e60d89236a244",
          lastModified  = "2020-07-21T13:16:42.859Z"
        ),
        ObjectSummary(
          location      = "something/23265eab-268e-4fcc-904f-775586b362c2",
          contentLength = 49,
          contentMd5    = "a3c2f1e38701bd2c7b54ebd7b1cd0dbc",
          lastModified  = "2020-07-21T13:16:41.226Z"
        )
      )
    }

    "return a ObjectListing with no objectSummaries" in {
      val location = generateLocation()

      listObjectsStub(location, statusCode = 200, Some(emptyObjectListingJson))

      osClient.listObjects(location).futureValue shouldBe ObjectListing(List.empty)
    }

    "return an exception if object-store response is not successful" in {
      val location = generateLocation()
      listObjectsStub(location, statusCode = 401, None)
      osClient.listObjects(location).failed.futureValue shouldBe an[UpstreamErrorResponse]
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

  private def generateLocation(): String = UUID.randomUUID().toString

  private def generateContent(body: String): Source[ByteString, NotUsed] =
    Source.single(ByteString(body.getBytes("UTF-8")))

  private def objectListingJson: String =
    """{
      |  "objects": [
      |    {
      |      "location": "something/0993180f-8f31-41b2-905c-71f0273bb7d4",
      |      "contentLength": 49,
      |      "contentMD5": "4033ff85a6fdc6a2f51e60d89236a244",
      |      "lastModified": "2020-07-21T13:16:42.859Z"
      |    },
      |    {
      |      "location": "something/23265eab-268e-4fcc-904f-775586b362c2",
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

  private def putObjectStub(location: String, statusCode: Int, reqBody: String, md5Base64: String): Unit = {
    val request = put(urlEqualTo(s"/object-store/object/$location"))
      .withHeader("content-length", equalTo("49"))
      .withRequestBody(binaryEqualTo(reqBody.getBytes))
      .withHeader("Content-Type", equalTo("application/octet-stream"))
      .withHeader("Content-MD5", equalTo(md5Base64))

    val response = aResponse().withStatus(statusCode)
    stubFor(
      request
        .willReturn(response))
  }

  private def getObjectStub(location: String, statusCode: Int, resBody: Option[String]): Unit = {
    val request = get(urlEqualTo(s"/object-store/object/$location"))
    val responseBuilder = aResponse.withStatus(statusCode)
    resBody.foreach(responseBuilder.withBody)

    stubFor(
      request
        .willReturn(responseBuilder))
  }

  private def deleteObjectStub(location: String, statusCode: Int = 200): Unit = {
    val request = delete(urlEqualTo(s"/object-store/object/$location"))
    val response = aResponse()
      .withStatus(statusCode)

    stubFor(
      request
        .willReturn(response))
  }

  private def listObjectsStub(location: String, statusCode: Int, resBodyJson: Option[String]): Unit = {
    val request = get(urlEqualTo(s"/object-store/list/$location"))
    val responseBuilder = aResponse().withStatus(statusCode)
    resBodyJson foreach { body =>
      responseBuilder.withBody(body)
      responseBuilder.withHeader("Content-Type", "application/json")
    }

    stubFor(
      request
        .willReturn(responseBuilder))
  }

}
