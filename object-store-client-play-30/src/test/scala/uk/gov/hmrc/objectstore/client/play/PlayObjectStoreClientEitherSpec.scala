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

package uk.gov.hmrc.objectstore.client.play

import java.net.URL
import java.time.Instant
import java.util.UUID

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.scalatest.{BeforeAndAfterAll, EitherValues}
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
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectSummary, ObjectListing, ObjectSummaryWithMd5, Path, RetentionPeriod}
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.http.Payload
import uk.gov.hmrc.objectstore.client.utils.PathUtils._
import uk.gov.hmrc.objectstore.client.wiremock.ObjectStoreStubs._
import java.util.UUID.randomUUID

import scala.concurrent.ExecutionContextExecutor

class PlayObjectStoreClientEitherSpec
  extends AnyWordSpec
     with Matchers
     with GuiceOneServerPerSuite
     with BeforeAndAfterAll
     with WireMockSupport
     with ScalaFutures
     with IntegrationPatience
     with EitherValues {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val system: ActorSystem          = ActorSystem()

  private val application: Application = fakeApplication()
  protected val osClient: PlayObjectStoreClientEither =
    application.injector.instanceOf(classOf[PlayObjectStoreClientEither])

  lazy val owner = "my-service"

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[ObjectStoreClientConfig].toInstance(
          ObjectStoreClientConfig(
            baseUrl                = wireMockUrl,
            owner                  = owner,
            authorizationToken     = "AuthorizationToken",
            defaultRetentionPeriod = RetentionPeriod.OneWeek
          )
        )
      )
      .build()

  import Implicits._

  "putObject" must {
    val summary =
      ObjectSummaryWithMd5(
        location      = Path.File(Path.Directory("zips"), "zip1.zip"),
        contentLength = 1000L,
        contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
        lastModified  = Instant.now
      )

    "store an object as Source with NotUsed bound to Mat" in {
      val body                                = s"hello world! ${UUID.randomUUID().toString}"
      val path                                = generateFilePath()
      val md5Base64                           = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, NotUsed] = toSource(body)

      initPutObjectStub(path, body.getBytes, md5Base64, owner = owner, statusCode = 200, response = Some(summary))

      osClient.putObject(path, source).futureValue.value shouldBe summary
    }

    "store an object as Source with Any bound to Mat" in {
      val body                          = s"hello world! ${UUID.randomUUID().toString}"
      val path                          = generateFilePath()
      val md5Base64                     = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, _] = toSource(body)

      initPutObjectStub(path, body.getBytes, md5Base64, owner = owner, statusCode = 200, response = Some(summary))

      osClient.putObject(path, source).futureValue.value shouldBe summary
    }

    "store an object as Source with NotUsed bound to Mat and known md5hash and length" in {
      val body                                = s"hello world! ${UUID.randomUUID().toString}"
      val path                                = generateFilePath()
      val md5Base64                           = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, NotUsed] = toSource(body)

      initPutObjectStub(path, body.getBytes, md5Base64, owner = owner, statusCode = 200, response = Some(summary))

      osClient
        .putObject(path, Payload(length = body.length, md5Hash = md5Base64, content = source))
        .futureValue
        .value shouldBe summary
    }

    "store an object as Source with Any bound to Mat and known md5hash and length" in {
      val body                          = s"hello world! ${UUID.randomUUID().toString}"
      val path                          = generateFilePath()
      val md5Base64                     = Md5HashUtils.fromBytes(body.getBytes)
      val source: Source[ByteString, _] = toSource(body)

      initPutObjectStub(path, body.getBytes, md5Base64, owner = owner, statusCode = 200, response = Some(summary))

      osClient
        .putObject(path, Payload(length = body.length, md5Hash = md5Base64, content = source))
        .futureValue
        .value shouldBe summary
    }

    "store an object as Bytes" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}".getBytes
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body)

      initPutObjectStub(path, body, md5Base64, owner = owner, statusCode = 200, response = Some(summary))

      osClient.putObject(path, body).futureValue.value shouldBe summary
    }

    "store an object with explicit retention period" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}".getBytes
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body)

      initPutObjectStub(
        path,
        body,
        md5Base64,
        owner           = owner,
        retentionPeriod = RetentionPeriod.OneMonth,
        statusCode      = 200,
        response        = Some(summary)
      )

      osClient.putObject(path, body, RetentionPeriod.OneMonth).futureValue.value shouldBe summary
    }

    "store an object as String" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}"
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body.getBytes)

      initPutObjectStub(path, body.getBytes, md5Base64, owner = owner, statusCode = 200, response = Some(summary))

      osClient.putObject(path, body).futureValue.value shouldBe summary
    }

    "return an exception if object-store response is not successful" in {
      val body      = s"hello world! ${UUID.randomUUID().toString}"
      val path      = generateFilePath()
      val md5Base64 = Md5HashUtils.fromBytes(body.getBytes)

      initPutObjectStub(path, body.getBytes, md5Base64, owner = owner, statusCode = 401, response = None)

      osClient.putObject(path, toSource(body)).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "getObject" must {
    "return an object that exists" in {
      val body = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = owner)

      val obj = osClient.getObject[Source[ByteString, NotUsed]](path).futureValue.value
      obj.get.content.asString() shouldBe body
    }

    "return an object that exists as String" in {
      val body = "hello world! e36cb887-58ae-4422-9894-215faaf0aa35"
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = owner)

      import InMemoryReads.stringContentRead

      val obj = osClient.getObject[String](path).futureValue.value
      obj.get.content shouldBe body
    }

    case class Obj(k1: String, k2: String)
    implicit val or: Reads[Obj] =
      ( (__ \ "k1").read[String]
      ~ (__ \ "k2").read[String]
      )(Obj.apply _)

    "return an object that exists as JsValue" in {
      val body = """{ "k1": "v1", "k2": "v2" }"""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = owner)

      import InMemoryReads.jsValueContentRead

      val obj = osClient.getObject[JsValue](path).futureValue.value
      obj.get.content shouldBe JsObject(Seq("k1" -> JsString("v1"), "k2" -> JsString("v2")))
    }

    "fail with invalid json when reading as JsValue" in {
      val body = """{ "k1": "v1", "k2": "v2""""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = owner)

      import InMemoryReads.jsValueContentRead

      osClient.getObject[JsValue](path).futureValue.left.value shouldBe an[Exception]
    }

    "return an object that exists as JsResult" in {
      val body = """{ "k1": "v1", "k2": "v2" }"""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = owner)

      import InMemoryReads._

      val obj = osClient.getObject[JsResult[Obj]](path).futureValue.value
      obj.get.content shouldBe JsSuccess(Obj(k1 = "v1", k2 = "v2"), __)
    }

    "return an object that exists as JsReads" in {
      val body = """{ "k1": "v1", "k2": "v2" }"""
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 200, Some(body), owner = owner)

      import InMemoryReads._

      val obj = osClient.getObject[Obj](path).futureValue.value
      obj.get.content shouldBe Obj(k1 = "v1", k2 = "v2")
    }

    "return None for an object that doesn't exist" in {
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 404, None, owner = owner)

      osClient.getObject(path).futureValue.value shouldBe None
    }

    "return an exception if object-store response is not successful" in {
      val path = generateFilePath()

      initGetObjectStub(path, statusCode = 401, None, owner = owner)

      osClient.getObject(path).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "deleteObject" must {
    "delete an object" in {
      val path = generateFilePath()

      initDeleteObjectStub(path, owner = owner)

      osClient.deleteObject(path).futureValue.value shouldBe ((): Unit)
    }

    "return an exception if object-store response is not successful" in {
      val path = generateFilePath()

      initDeleteObjectStub(path, statusCode = 401, owner = owner)

      osClient.deleteObject(path).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "listObject" must {
    "return a ObjectListing with objects" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 200, Some(objectListingJson), owner = owner)

      osClient.listObjects(path).futureValue.value.objectSummaries shouldBe List(
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "0993180f-8f31-41b2-905c-71f0273bb7d4"),
          contentLength = 49,
          lastModified  = Instant.parse("2020-07-21T13:16:42.859Z")
        ),
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "23265eab-268e-4fcc-904f-775586b362c2"),
          contentLength = 49,
          lastModified  = Instant.parse("2020-07-21T13:16:41.226Z")
        )
      )
    }

    "return an ObjectListing with objects for owner's root directory" in {
      val path = Path.Directory("")

      initListObjectsStub(path, statusCode = 200, Some(objectListingJson), owner)

      osClient.listObjects(path).futureValue.value.objectSummaries shouldBe List(
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "0993180f-8f31-41b2-905c-71f0273bb7d4"),
          contentLength = 49,
          lastModified  = Instant.parse("2020-07-21T13:16:42.859Z")
        ),
        ObjectSummary(
          location      = Path.File(Path.Directory("something"), "23265eab-268e-4fcc-904f-775586b362c2"),
          contentLength = 49,
          lastModified  = Instant.parse("2020-07-21T13:16:41.226Z")
        )
      )
    }

    "return a ObjectListing with no objects" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 200, Some(emptyObjectListingJson), owner = owner)

      osClient.listObjects(path).futureValue.value shouldBe ObjectListing(List.empty)
    }

    "return an exception if object-store response is not successful" in {
      val path = generateDirectoryPath()

      initListObjectsStub(path, statusCode = 401, None, owner = owner)

      osClient.listObjects(path).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "zip" must {
    "return an ObjectListing with objects" in {
      val from            = Path.Directory("envelope1")
      val to              = Path.File(Path.Directory("zips"), "zip1.zip")
      val retentionPeriod = RetentionPeriod.OneWeek

      val zipResponse =
        ObjectSummaryWithMd5(
          location      = Path.File(Path.Directory("object-store/object/zips"), "zip1.zip"),
          contentLength = 1000L,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.now
        )

      initZipStub(from, to, retentionPeriod, owner, owner, statusCode = 200, Some(zipResponse))

      osClient.zip(from, to, retentionPeriod).futureValue.value shouldBe zipResponse
    }

    "return an exception if object-store response is not successful" in {
      val from            = Path.Directory("envelope1")
      val to              = Path.File(Path.Directory("zips"), "zip1.zip")
      val retentionPeriod = RetentionPeriod.OneWeek

      initZipStub(from, to, retentionPeriod, owner, owner, statusCode = 401, response = None)

      osClient.zip(from, to, retentionPeriod).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  "uploadFromUrl" must {
    "return an ObjectListing with objectSummaries" in {
      val from            = new URL("https://fus-outbound-8264ee52f589f4c0191aa94f87aa1aeb.s3.eu-west-2.amazonaws.com/81fb03f5-195d-422a-91ab-460939045846")
      val to              = Path.File(Path.Directory("my-folder"), "sample.pdf")
      val retentionPeriod = RetentionPeriod.OneWeek

      val response =
        ObjectSummaryWithMd5(
          location      = Path.File(Path.Directory("object-store/object/my-folder"), "sample.pdf"),
          contentLength = 1000L,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.now
        )

      initUploadFromUrlStub(from, to, retentionPeriod, None, None, owner, statusCode = 200, Some(response))

      osClient.uploadFromUrl(from, to, retentionPeriod).futureValue.value shouldBe response
    }

    "return an ObjectListing with objectSummaries when contentType supplied" in {
      val from            = new URL("https://fus-outbound-8264ee52f589f4c0191aa94f87aa1aeb.s3.eu-west-2.amazonaws.com/81fb03f5-195d-422a-91ab-460939045846")
      val to              = Path.File(Path.Directory("my-folder"), "sample.pdf")
      val retentionPeriod = RetentionPeriod.OneWeek
      val contentType     = Some("text/csv")

      val response =
        ObjectSummaryWithMd5(
          location      = Path.File(Path.Directory("object-store/object/my-folder"), "sample.pdf"),
          contentLength = 1000L,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.now
        )

      initUploadFromUrlStub(from, to, retentionPeriod, contentType, None, owner, statusCode = 200, Some(response))

      osClient.uploadFromUrl(from, to, retentionPeriod, contentType).futureValue.value shouldBe response
    }

    "return an ObjectListing with objectSummaries when contentMd5 supplied" in {
      val from            = new URL("https://fus-outbound-8264ee52f589f4c0191aa94f87aa1aeb.s3.eu-west-2.amazonaws.com/81fb03f5-195d-422a-91ab-460939045846")
      val to              = Path.File(Path.Directory("my-folder"), "sample.pdf")
      val retentionPeriod = RetentionPeriod.OneWeek
      val contentType     = Some("text/csv")
      val contentMd5      = Some(Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"))

      val response =
        ObjectSummaryWithMd5(
          location      = Path.File(Path.Directory("object-store/object/my-folder"), "sample.pdf"),
          contentLength = 1000L,
          contentMd5    = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
          lastModified  = Instant.now
        )

      initUploadFromUrlStub(from, to, retentionPeriod, contentType, contentMd5 , owner, statusCode = 200, Some(response))

      osClient.uploadFromUrl(from, to, retentionPeriod, contentType, contentMd5).futureValue.value shouldBe response
    }

    "return an exception if object-store response is not successful" in {
      val from            = new URL("https://fus-outbound-8264ee52f589f4c0191aa94f87aa1aeb.s3.eu-west-2.amazonaws.com/81fb03f5-195d-422a-91ab-460939045846")
      val to              = Path.File(Path.Directory("my-folder"), "sample.pdf")
      val retentionPeriod = RetentionPeriod.OneWeek

      initUploadFromUrlStub(from, to, retentionPeriod, None, None, owner, statusCode = 401, None)

      osClient.uploadFromUrl(from, to, retentionPeriod).futureValue.left.value shouldBe an[UpstreamErrorResponse]
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization(randomUUID().toString)))

  override def afterAll(): Unit = {
    super.afterAll()
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
        "objectSummaries": [
          {
            "location": "/object-store/object/something/0993180f-8f31-41b2-905c-71f0273bb7d4",
            "contentLength": 49,
            "lastModified": "2020-07-21T13:16:42.859Z"
          },
          {
            "location": "/object-store/object/something/23265eab-268e-4fcc-904f-775586b362c2",
            "contentLength": 49,
            "lastModified": "2020-07-21T13:16:41.226Z"
          }
        ]
      }"""

  private def emptyObjectListingJson: String =
    """{
          "objectSummaries": []
      }"""
}
