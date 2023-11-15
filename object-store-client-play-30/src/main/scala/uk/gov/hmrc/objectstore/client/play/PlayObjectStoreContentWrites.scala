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

import java.io.{File, FileInputStream}

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.{ClosedShape, Materializer}
import org.apache.pekko.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, RunnableGraph, Sink, Source}
import org.apache.pekko.util.ByteString
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.http.{ObjectStoreContentWrite, Payload}
import play.api.libs.ws.BodyWritable

import scala.concurrent.{ExecutionContext, Future}

trait PlayObjectStoreContentWrites {
  private def bodyWritable[T](contentType: Option[String])(implicit bw: BodyWritable[T]): BodyWritable[T] =
    contentType.fold(bw)(ct => BodyWritable(bw.transform, ct))

  implicit def payloadAkkaSourceContentWrite[F[_], Mat <: Any](implicit
    F: PlayMonad[F]
  ): ObjectStoreContentWrite[F, Payload[Source[ByteString, Mat]], Request] =
    new ObjectStoreContentWrite[F, Payload[Source[ByteString, Mat]], Request] {
      override def writeContent(
        payload    : Payload[Source[ByteString, Mat]],
        contentType: Option[String],
        contentMd5 : Option[Md5Hash]
      ): F[Request] =
        if (contentMd5.exists(payload.md5Hash != _))
          F.raiseError(new RuntimeException(s"Content Md5 did not match"))
        else
          F.pure(
            HttpBody(
              length    = Some(payload.length),
              md5       = Some(payload.md5Hash),
              writeBody = (req: WSRequest) => req.withBody(payload.content)(bodyWritable(contentType)),
              release   = () => ()
            )
          )
    }

  implicit def fileWrite[F[_]](implicit F: PlayMonad[F]): ObjectStoreContentWrite[F, File, Request] =
    payloadAkkaSourceContentWrite[F, NotUsed].contramap { file =>
      Payload(
        length = file.length,
        md5Hash = Md5HashUtils.fromInputStream(new FileInputStream(file)),
        content = FileIO.fromPath(file.toPath).mapMaterializedValue(_ => NotUsed)
      )
    }

  implicit def akkaSourceContentWrite[F[_], Mat <: Any](implicit
    ec: ExecutionContext,
    m: Materializer,
    F: PlayMonad[F]
  ): ObjectStoreContentWrite[F, Source[ByteString, Mat], Request] =
    new ObjectStoreContentWrite[F, Source[ByteString, Mat], Request] {
      override def writeContent(
        content    : Source[ByteString, Mat],
        contentType: Option[String],
        contentMd5 : Option[Md5Hash]
      ): F[Request] = {
        val tempFile = SingletonTemporaryFileCreator.create()

        val (uploadFinished, md5Finished) =
          broadcast2(
            source = content,
            sink1 = FileIO.toPath(tempFile.path),
            sink2 = Md5HashUtils.md5HashSink
          ).run()

        F.liftFuture(
          for {
            _       <- uploadFinished
            md5Hash <- md5Finished
            _       <- if (contentMd5.exists(md5Hash != _))
                         Future.failed(new RuntimeException(s"Content Md5 did not match"))
                       else Future.unit
          } yield HttpBody(
            length    = Some(tempFile.path.toFile.length),
            md5       = Some(md5Hash),
            writeBody = (req: WSRequest) => req.withBody(tempFile.path.toFile)(bodyWritable(contentType)),
            release   = () => SingletonTemporaryFileCreator.delete(tempFile)
          )
        )
      }
    }

  private def broadcast2[T, Mat1, Mat2](
    source: Source[T, Any],
    sink1: Sink[T, Mat1],
    sink2: Sink[T, Mat2]
  ): RunnableGraph[(Mat1, Mat2)] =
    RunnableGraph.fromGraph(GraphDSL.create(sink1, sink2)(Tuple2.apply) { implicit builder => (s1, s2) =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[T](outputPorts = 2))
      source ~> broadcast
      broadcast.out(0) ~> Flow[T].async ~> s1
      broadcast.out(1) ~> Flow[T].async ~> s2
      ClosedShape
    })

  implicit def bytesWrite[F[_]](implicit F: PlayMonad[F]): ObjectStoreContentWrite[F, Array[Byte], Request] =
    payloadAkkaSourceContentWrite[F, NotUsed].contramap { bytes =>
      Payload(
        length = bytes.length,
        md5Hash = Md5HashUtils.fromBytes(bytes),
        content = Source.single(bytes).map(ByteString(_))
      )
    }

  implicit def stringWrite[F[_]](implicit F: PlayMonad[F]): ObjectStoreContentWrite[F, String, Request] =
    bytesWrite.contramap(_.getBytes)
}
