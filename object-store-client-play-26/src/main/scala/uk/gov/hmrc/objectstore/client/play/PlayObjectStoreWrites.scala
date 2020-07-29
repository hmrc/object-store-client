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

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.objectstore.client.model.http.ObjectStoreWrite
import uk.gov.hmrc.objectstore.client.play.PlayWSHttpClient.Request

import scala.concurrent.{ExecutionContext, Future}

trait PlayObjectStoreWrites {
  implicit def akkaSourceWrite(implicit ec: ExecutionContext, m: Materializer): ObjectStoreWrite[Source[ByteString, akka.NotUsed], Request] =
    new ObjectStoreWrite[Source[ByteString, akka.NotUsed], Request] {
      override def write(body: Source[ByteString, NotUsed]): Request = {
        val tempFile = SingletonTemporaryFileCreator.create()
        body.runWith(FileIO.toPath(tempFile.path)).map { _ =>

          HttpBody(
            length = Some(tempFile.path.toFile.length),
            md5 = Some(Md5Hash.fromInputStream(new FileInputStream(tempFile.path.toFile))),
            writeBody = (req: WSRequest) => req.withBody(body),
            release = () => SingletonTemporaryFileCreator.delete(tempFile)
          )
        }
      }
    }

  implicit lazy val stringWrite: ObjectStoreWrite[String, Request] =
    new ObjectStoreWrite[String, Request] {
      override def write(body: String): Request = Future.successful(HttpBody(
        length = Some(body.getBytes.length),
        md5 = Some(Md5Hash.fromInputStream(new java.io.ByteArrayInputStream(body.getBytes))),
        writeBody = (req: WSRequest) => req.withBody(body.getBytes),
        release = () => ())
      )
    }
}

object PlayObjectStoreWrites extends PlayObjectStoreWrites
