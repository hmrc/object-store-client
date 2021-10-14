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

package uk.gov.hmrc.objectstore.client.play

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.objectstore.client.RetentionPeriod.OneWeek
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.http.Payload
import uk.gov.hmrc.objectstore.client.utils.PathUtils._
import uk.gov.hmrc.objectstore.client.wiremock.ObjectStoreStubs._
import uk.gov.hmrc.objectstore.client.wiremock.WireMockHelper
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectListing, ObjectSummary, Path, RetentionPeriod, ZipRequest}

import java.time.Instant
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContextExecutor

class PlayObjectStoreClientSpec
  extends AnyWordSpec
     with Matchers
     with GuiceOneServerPerSuite
     with BeforeAndAfterAll
     with WireMockHelper
     with ScalaFutures
     with IntegrationPatience {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val system: ActorSystem          = ActorSystem()
  implicit val m: ActorMaterializer         = ActorMaterializer()

  private val application: Application          = fakeApplication()
  protected val osClient: PlayObjectStoreClient = application.injector.instanceOf(classOf[PlayObjectStoreClient])

  lazy val defaultOwner = "my-service"

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[ObjectStoreClientConfig].toInstance(
          ObjectStoreClientConfig(
            baseUrl = wireMockUrl,
            owner = defaultOwner,
            authorizationToken = "AuthorizationToken",
            defaultRetentionPeriod = RetentionPeriod.OneWeek
          )
        )
      )
      .build()

  import Implicits._

  "putObject" must {

    "store an object as Source with NotUsed bound to Mat" in {
      val body                                = s"hello world! ${randomUUID().toString}"
      val path                                = generateFilePath()
      val md5Base64                           = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, NotUsed] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64, owner = defaultOwner)

      osClient.putObject(path, source).futureValue shouldBe ((): Unit)
    }

    "store an object as Source with Any bound to Mat" in {
      val body                          = s"hello world! ${randomUUID().toString}"
      val path                          = generateFilePath()
      val md5Base64                     = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, _] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64, owner = defaultOwner)

      osClient.putObject(path, source).futureValue shouldBe ((): Unit)
    }

    "store an object as Source with NotUsed bound to Mat and known md5hash and length" in {
      val body                                = s"hello world! ${randomUUID().toString}"
      val path                                = generateFilePath()
      val md5Base64                           = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, NotUsed] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64, owner = defaultOwner)

      osClient
        .putObject(path, Payload(length = body.length, md5Hash = md5Base64, content = source))
        .futureValue shouldBe ((): Unit)
    }

    "store an object as Source with Any bound to Mat and known md5hash and length" in {
      val body                          = s"hello world! ${randomUUID().toString}"
      val path                          = generateFilePath()
      val md5Base64                     = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, _] = toSource(body)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64, owner = defaultOwner)

      osClient
        .putObject(path, Payload(length = body.length, md5Hash = md5Base64, content = source))
        .futureValue shouldBe ((): Unit)
    }

    "store an object as Bytes" in {
      val body      = s"hello world! ${randomUUID().toString}".getBytes
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body)

      initPutObjectStub(path, statusCode = 201, body, md5Base64, owner = defaultOwner)

      osClient.putObject(path, body).futureValue shouldBe ((): Unit)
    }

    "store an object with explicit retention period" in {
      val body      = s"hello world! ${randomUUID().toString}".getBytes
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body)

      initPutObjectStub(
        path,
        statusCode = 201,
        body,
        md5Base64,
        owner = defaultOwner,
        retentionPeriod = RetentionPeriod.OneMonth
      )

      osClient.putObject(path, body, RetentionPeriod.OneMonth).futureValue shouldBe ((): Unit)
    }

    "store an object as String" in {
      val body      = s"hello world! ${randomUUID().toString}"
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body.getBytes)

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64, owner = defaultOwner)

      osClient.putObject(path, body, OneWeek).futureValue shouldBe ((): Unit)
    }

    "return an exception if object-store response is not successful" in {
      val body      = s"hello world! ${randomUUID().toString}"
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body.getBytes)

      initPutObjectStub(path, statusCode = 401, body.getBytes, md5Base64, owner = defaultOwner)

      osClient.putObject(path, toSource(body)).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }

    "store an object with differerent owner" in {
      val body      = s"hello world! ${randomUUID().toString}"
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body.getBytes)
      val owner     = "my-owner"

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64, owner = owner)

      osClient.putObject(path, body, owner = owner).futureValue shouldBe ((): Unit)
    }

    "store an object with a specified content-type" in {
      val body        = s"hello world! ${randomUUID().toString}"
      val path        = generateFilePath()
      val md5Base64   = Md5HashUtils.fromBytes(body.getBytes)
      val contentType = "application/mycontenttype"
      val owner       = "my-owner"

      initPutObjectStub(path, statusCode = 201, body.getBytes, md5Base64, owner, contentType = contentType)

      osClient.putObject(path, body, contentType = Some(contentType), owner = owner).futureValue shouldBe ((): Unit)
    }
  }

  "getObject" must {
    "return an object that exists" in {
      val body = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = defaultOwner)

      val obj = osClient.getObject[Source[ByteString, NotUsed]](path).futureValue
      obj.get.content.asString() shouldBe body
    }

    "return an object that exists as String" in {
      val body = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = defaultOwner)

      import InMemoryReads.stringContentRead

      val obj = osClient.getObject[String](path).futureValue
      obj.get.content shouldBe body
    }

    case class Obj(k1: String, k2: String)
    implicit val or: Reads[Obj] =
      ((__ \ "k1").read[String]
        ~ (__ \ "k2").read[String])(Obj.apply _)

    "return an object that exists as JsValue" in {
      val body = """{ "k1": "v1", "k2": "v2" }"""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = defaultOwner)

      import InMemoryReads.jsValueContentRead

      val obj = osClient.getObject[JsValue](path).futureValue
      obj.get.content shouldBe JsObject(Seq("k1" -> JsString("v1"), "k2" -> JsString("v2")))
    }

    // TODO what's the expected behaviour here?
    "fail with invalid json when reading as JsValue" in {
      val body = """{ "k1": "v1", "k2": "v2""""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = defaultOwner)

      import InMemoryReads.jsValueContentRead

      osClient.getObject[JsValue](path).failed.futureValue shouldBe an[Exception]
    }

    "return an object that exists as JsResult" in {
      val body = """{ "k1": "v1", "k2": "v2" }"""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = defaultOwner)

      import InMemoryReads._

      val obj = osClient.getObject[JsResult[Obj]](path).futureValue
      obj.get.content shouldBe JsSuccess(Obj(k1 = "v1", k2 = "v2"), __)
    }

    "return an object that exists as JsReads" in {
      val body = """{ "k1": "v1", "k2": "v2" }"""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = defaultOwner)

      import InMemoryReads._

      val obj = osClient.getObject[Obj](path).futureValue
      obj.get.content shouldBe Obj(k1 = "v1", k2 = "v2")
    }

    "return None for an object that doesn't exist" in {
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 404, None, owner = defaultOwner)

      osClient.getObject(path).futureValue shouldBe None
    }

    "return an exception if object-store response is not successful" in {
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 401, None, owner = defaultOwner)

      osClient.getObject(path).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }

    "return an object with different owner" in {
      val body  = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val path  = generateFilePath()
      val owner = "my-owner"

      initGetObjectStub(path, statusCode = 200, Some(body), owner)

      val obj = osClient.getObject[Source[ByteString, NotUsed]](path, owner).futureValue
      obj.get.content.asString() shouldBe body
    }
  }

  "deleteObject" must {
    "delete an object" in {
      val path = generateFilePath()

      initDeleteObjectStub(path, owner = defaultOwner)

      osClient.deleteObject(path).futureValue shouldBe ((): Unit)
    }

    "return an exception if object-store response is not successful" in {
      val path = generateFilePath()

      initDeleteObjectStub(path, statusCode = 401, owner = defaultOwner)

      osClient.deleteObject(path).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }

    "delete an object with different owner" in {
      val path  = generateFilePath()
      val owner = "my-owner"

      initDeleteObjectStub(path, owner = owner)

      osClient.deleteObject(path, owner).futureValue shouldBe ((): Unit)
    }
  }

  "listObject" must {
    "return an ObjectListing with objectSummaries" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 200, Some(objectListingJson), owner = defaultOwner)

      osClient.listObjects(path).futureValue.objectSummaries shouldBe List(
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "0993180f-8f31-41b2-905c-71f0273bb7d4"),
          contentLength = 49,
          contentMd5    = Md5Hash("4033ff85a6fdc6a2f51e60d89236a244"),
          lastModified  = Instant.parse("2020-07-21T13:16:42.859Z")
        ),
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "23265eab-268e-4fcc-904f-775586b362c2"),
          contentLength = 49,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.parse("2020-07-21T13:16:41.226Z")
        )
      )
    }

    "return an ObjectListing with objectSummaries for owner's root directory" in {
      val path = Path.Directory("")

      initListObjectsStub(path, statusCode = 200, Some(objectListingJson), defaultOwner)

      osClient.listObjects(path).futureValue.objectSummaries shouldBe List(
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "0993180f-8f31-41b2-905c-71f0273bb7d4"),
          contentLength = 49,
          contentMd5    = Md5Hash("4033ff85a6fdc6a2f51e60d89236a244"),
          lastModified  = Instant.parse("2020-07-21T13:16:42.859Z")
        ),
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "23265eab-268e-4fcc-904f-775586b362c2"),
          contentLength = 49,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.parse("2020-07-21T13:16:41.226Z")
        )
      )
    }

    "return a ObjectListing with no objectSummaries" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 200, Some(emptyObjectListingJson), owner = defaultOwner)

      osClient.listObjects(path).futureValue shouldBe ObjectListing(List.empty)
    }

    "return an exception if object-store response is not successful" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 401, None, owner = defaultOwner)

      osClient.listObjects(path).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }

    "return an ObjectListing with different owner" in {
      val path  = generateDirectoryPath()
      val owner = "my-owner"

      initListObjectsStub(path, statusCode = 200, Some(objectListingJson), owner)

      osClient.listObjects(path, owner).futureValue.objectSummaries shouldBe List(
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "0993180f-8f31-41b2-905c-71f0273bb7d4"),
          contentLength = 49,
          contentMd5    = Md5Hash("4033ff85a6fdc6a2f51e60d89236a244"),
          lastModified  = Instant.parse("2020-07-21T13:16:42.859Z")
        ),
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "23265eab-268e-4fcc-904f-775586b362c2"),
          contentLength = 49,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.parse("2020-07-21T13:16:41.226Z")
        )
      )
    }
  }

  "zip" must {
    "return an ObjectListing with objectSummaries" in {
      val zipRequest =
        ZipRequest(
          from            = Path.Directory("envelope1"),
          to              = Path.File(Path.Directory("zips"), "zip1.zip"),
          retentionPeriod = RetentionPeriod.OneWeek
        )

      val zipResponse =
        ObjectSummary(
          location      = Path.File(Path.Directory("zips"), "zip1.zip"),
          contentLength = 1000L,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.now
        )

      initZipStub(zipRequest, statusCode = 200, Some(zipResponse))

      osClient.zip(zipRequest).futureValue shouldBe zipResponse
    }

    "return an exception if object-store response is not successful" in {
      val zipRequest =
        ZipRequest(
          from            = Path.Directory("envelope1"),
          to              = Path.File(Path.Directory("zips"), "zip1.zip"),
          retentionPeriod = RetentionPeriod.OneWeek
        )

      initZipStub(zipRequest, statusCode = 401, response = None)

      osClient.zip(zipRequest).failed.futureValue shouldBe an[UpstreamErrorResponse]
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization(randomUUID().toString)))

  override def afterAll: Unit = {
    super.afterAll
    application.stop()
    system.terminate()
  }

  implicit class SourceOps(source: Source[ByteString, _]) {

    def asString(): String =
      source.map(_.utf8String).runReduce(_ + _).futureValue
  }

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
}
