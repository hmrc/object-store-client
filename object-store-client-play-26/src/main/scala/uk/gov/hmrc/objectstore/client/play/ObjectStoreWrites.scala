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

import java.io.FileInputStream

import akka.stream.scaladsl.Source
import akka.util.ByteString
import uk.gov.hmrc.objectstore.client.model.http.{ObjectStoreWrite, ObjectStoreWriteData}

import scala.concurrent.{ExecutionContext, Future}

trait ObjectStoreWrites {
  implicit def akkaObjectStoreWrite(implicit ec: ExecutionContext, m: akka.stream.Materializer): ObjectStoreWrite[Source[ByteString, akka.NotUsed]] =
    new ObjectStoreWrite[Source[ByteString, akka.NotUsed]] {
      import play.api.libs.Files.SingletonTemporaryFileCreator
      import akka.stream.scaladsl.{FileIO, Source, StreamConverters}

      override def write(body: Source[ByteString, akka.NotUsed]): Future[ObjectStoreWriteData] = {
        val tempFile = SingletonTemporaryFileCreator.create()
        // TODO can we calculate md5Hash on same pass as writing stream to local file? (e.g. DigestCalculator https://doc.akka.io/docs/akka/current/stream/stream-cookbook.html)
        body.runWith(FileIO.toPath(tempFile.path)).map { _ =>
          ObjectStoreWriteData.Stream(
            stream        = FileIO.fromPath(tempFile.path).map(_.toArray).runWith(StreamConverters.asJavaStream[Array[Byte]]()),
            contentLength = tempFile.path.toFile.length,
            md5Hash       = Md5Hash.fromInputStream(new FileInputStream(tempFile.path.toFile)),
            release       = () => SingletonTemporaryFileCreator.delete(tempFile)
          )
        }
      }
    }

  implicit lazy val stringObjectStoreWrite: ObjectStoreWrite[String] =
    new ObjectStoreWrite[String] {
      override def write(body: String): Future[ObjectStoreWriteData] =
        Future.successful(
          ObjectStoreWriteData.InMemory(body.getBytes)
        )
    }
}

object ObjectStoreWrites extends ObjectStoreWrites
