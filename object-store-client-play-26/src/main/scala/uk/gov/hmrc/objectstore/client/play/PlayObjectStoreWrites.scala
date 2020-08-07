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

import java.io.{File, FileInputStream}

import akka.NotUsed
import akka.stream.{ClosedShape, Materializer}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.util.ByteString
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.objectstore.client.model.http.{ObjectStoreContentWrite, Payload}
import uk.gov.hmrc.objectstore.client.play.PlayWSHttpClient.Request

import scala.concurrent.{ExecutionContext, Future}

trait PlayObjectStoreContentWrites {

  implicit val payloadAkkaSourceContentWrite: ObjectStoreContentWrite[Payload[Source[ByteString, NotUsed]], Request] =
    new ObjectStoreContentWrite[Payload[Source[ByteString, NotUsed]], Request] {
      override def writeContent(payload: Payload[Source[ByteString, NotUsed]]): Request =
        Future.successful(
          HttpBody(
            length    = Some(payload.length),
            md5       = Some(payload.md5Hash),
            writeBody = (req: WSRequest) => req.withBody(payload.content),
            release   = () => ()
          )
        )
    }

  implicit val fileWrite: ObjectStoreContentWrite[File, Request] =
    payloadAkkaSourceContentWrite.contramap { file =>
      Payload(
        length  = file.length,
        md5Hash = Md5Hash.fromInputStream(new FileInputStream(file)),
        content = FileIO.fromPath(file.toPath).mapMaterializedValue(_ => NotUsed)
      )
    }

  implicit def akkaSourceContentWrite(implicit ec: ExecutionContext, m: Materializer): ObjectStoreContentWrite[Source[ByteString, NotUsed], Request] =
    new ObjectStoreContentWrite[Source[ByteString, NotUsed], Request] {
      override def writeContent(content: Source[ByteString, NotUsed]): Request = {
        val tempFile = SingletonTemporaryFileCreator.create()

        val (uploadFinished, md5Finished) =
          broadcast2(
            source = content,
            sink1  = FileIO.toPath(tempFile.path),
            sink2  = Md5Hash.md5HashSink
          ).run()

        for {
          _       <- uploadFinished
          md5Hash <- md5Finished
        } yield
          HttpBody(
            length    = Some(tempFile.path.toFile.length),
            md5       = Some(md5Hash),
            writeBody = (req: WSRequest) => req.withBody(content), // TODO check this isn't writing to disk too...
            release   = () => SingletonTemporaryFileCreator.delete(tempFile)
          )
      }
    }

  private def broadcast2[T, Mat1, Mat2](
    source: Source[T, Any],
    sink1: Sink[T, Mat1],
    sink2: Sink[T, Mat2]
  ): RunnableGraph[(Mat1, Mat2)] =
    RunnableGraph.fromGraph(GraphDSL.create(sink1, sink2)(Tuple2.apply) {
      implicit builder => (s1, s2) =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[T](outputPorts = 2))
        source ~> broadcast
        broadcast.out(0) ~> Flow[T].async ~> s1
        broadcast.out(1) ~> Flow[T].async ~> s2
        ClosedShape
    })

  implicit lazy val bytesWrite: ObjectStoreContentWrite[Array[Byte], Request] =
    payloadAkkaSourceContentWrite.contramap { bytes =>
      Payload(
        length  = bytes.length,
        md5Hash = Md5Hash.fromBytes(bytes),
        content = Source.single(bytes).map(ByteString(_)) // TODO check this isn't inefficiently writing to disk...
      )
    }

  implicit lazy val stringWrite: ObjectStoreContentWrite[String, Request] =
    bytesWrite.contramap(_.getBytes)
}

object PlayObjectStoreContentWrites extends PlayObjectStoreContentWrites
