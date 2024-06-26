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

package uk.gov.hmrc.objectstore.client.play.test

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.RetentionPeriod.OneWeek
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.{FutureEither, PlayMonad}
import uk.gov.hmrc.objectstore.client.play.test.stub.{StubObjectStoreClient, StubPlayObjectStoreClient, StubPlayObjectStoreClientEither}

import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

class ClientStubSpec
  extends AnyFlatSpec
     with ScalaFutures
     with IntegrationPatience
     with Matchers
     with OptionValues {

  "StubPlayObjectStoreClient" should behave like stubObjectStoreClient(new StubPlayObjectStoreClient(config))

  "StubPlayObjectStoreClientEither" should behave like stubObjectStoreClient(new StubPlayObjectStoreClientEither(config))

  def stubObjectStoreClient[F[_]: PlayMonad](stub: StubObjectStoreClient[F])(implicit tf: ToFuture[F]): Unit = {

    it should "return None when it has no objects for the path" in new Setup {
      tf.toFuture(stub.getObject[Source[ByteString, NotUsed]](defaultPath)).futureValue shouldBe empty
    }

    it should "return object when it has a matching object for the path" in new Setup {
      (for {
         _               <- stub.putObject(defaultPath, defaultContent)
         actualObject    <- stub.getObject[Source[ByteString, NotUsed]](defaultPath)
         actualContent   <- actualObject.value.content.runReduce(_ ++ _)
         expectedContent <- defaultContent.runReduce(_ ++ _)
       } yield {
         actualObject.value.location.asUri shouldBe s"$baseUrl/object-store/object/$owner/${defaultPath.asUri}"
         actualContent shouldBe expectedContent
       }
      ).futureValue
    }

    it should "delete object" in new Setup {
      (for {
        _      <- stub.putObject(defaultPath, defaultContent)
        _      <- stub.deleteObject(defaultPath)
        actual <- stub.getObject[Source[ByteString, NotUsed]](defaultPath)
       } yield actual shouldBe empty
      ).futureValue
    }

    it should "list objects" in new Setup {
      private val nodeA  = randomUUID().toString
      private val nodeB  = randomUUID().toString
      private val leafB1 = randomUUID().toString
      private val nodeC  = randomUUID().toString
      private val leafC1 = randomUUID().toString
      private val leafC2 = randomUUID().toString

      (for {
         _          <- stub.putObject(Path.File(s"$nodeA/$nodeB/$leafB1"), content())
         _          <- stub.putObject(Path.File(s"$nodeA/$nodeB/$nodeC/$leafC1"), content())
         _          <- stub.putObject(Path.File(s"$nodeA/$nodeB/$nodeC/$leafC2"), content())
         objectsAtA <- stub.listObjects(Path.Directory(nodeA))
         objectsAtB <- stub.listObjects(Path.Directory(s"$nodeA/$nodeB"))
         objectsAtC <- stub.listObjects(Path.Directory(s"$nodeA/$nodeB/$nodeC"))
       } yield {
         objectsAtA.objectSummaries.map(_.location) should contain theSameElementsAs List(
           Path.File(s"$owner/$nodeA/$nodeB/$leafB1"),
           Path.File(s"$owner/$nodeA/$nodeB/$nodeC/$leafC1"),
           Path.File(s"$owner/$nodeA/$nodeB/$nodeC/$leafC2")
         )
         objectsAtB.objectSummaries.map(_.location) should contain theSameElementsAs List(
           Path.File(s"$owner/$nodeA/$nodeB/$leafB1"),
           Path.File(s"$owner/$nodeA/$nodeB/$nodeC/$leafC1"),
           Path.File(s"$owner/$nodeA/$nodeB/$nodeC/$leafC2")
         )
         objectsAtC.objectSummaries.map(_.location) should contain theSameElementsAs List(
           Path.File(s"$owner/$nodeA/$nodeB/$nodeC/$leafC1"),
           Path.File(s"$owner/$nodeA/$nodeB/$nodeC/$leafC2")
         )
       }
      ).futureValue
    }

    implicit def toFuture[A](fa: F[A]): Future[A] = tf.toFuture(fa)
  }

  trait ToFuture[F[_]] {
    def toFuture[A](fa: F[A]): Future[A]
  }

  object ToFuture {
    implicit val future: ToFuture[Future] = new ToFuture[Future] {
      override def toFuture[A](fa: Future[A]): Future[A] = fa
    }

    implicit val futureEither: ToFuture[FutureEither] = new ToFuture[FutureEither] {
      override def toFuture[A](fa: FutureEither[A]): Future[A] =
        fa.flatMap {
          case Right(value) => Future.successful(value)
          case Left(value)  => Future.failed(value)
        }
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val defaultPath    = Path.File(s"${randomUUID().toString}/${randomUUID().toString}")
    val defaultContent = content()

    def content(value: String = randomUUID().toString) =
      Source.single(ByteString(value))
  }

  private lazy val baseUrl = s"baseUrl-${randomUUID().toString}"
  private lazy val owner   = s"owner-${randomUUID().toString}"
  private lazy val token   = s"token-${randomUUID().toString}"
  private lazy val config  = ObjectStoreClientConfig(baseUrl, owner, token, OneWeek)

  private implicit lazy val system: ActorSystem = ActorSystem()
}
