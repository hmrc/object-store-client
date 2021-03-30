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

package uk.gov.hmrc.objectstore.client.play.test

import akka.NotUsed
import akka.stream.Materializer
import play.api.libs.ws.SourceBody
import play.api.libs.ws.ahc._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client._
import uk.gov.hmrc.objectstore.client.category.Monad
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.http.{ObjectStoreContentRead, ObjectStoreContentWrite}
import uk.gov.hmrc.objectstore.client.play._

import java.time.Instant
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object stub {

  class StubPlayObjectStoreClient(val config: ObjectStoreClientConfig)(implicit
    m: Materializer,
    ec: ExecutionContext
  ) extends PlayObjectStoreClient(AhcWSClient(), config)
      with StubObjectStoreClient[Future] {
    protected val M        = Implicits.futureMonad
    protected val wsClient = AhcWSClient()
  }

  class StubPlayObjectStoreClientEither(val config: ObjectStoreClientConfig)(implicit
    m: Materializer,
    ec: ExecutionContext
  ) extends PlayObjectStoreClientEither(AhcWSClient(), config)
      with StubObjectStoreClient[FutureEither] {
    protected val M        = Implicits.futureEitherMonad
    protected val wsClient = AhcWSClient()
  }

  trait StubObjectStoreClient[F[_]] { self: ObjectStoreClient[F, Request, Response, ResBody] =>

    protected def wsClient: WSClient
    protected def M: Monad[F]

    def config: ObjectStoreClientConfig

    type Directory = String
    type Filename  = String
    private val objectStore = mutable.Map.empty[Directory, Map[Filename, InternalObject]]
    private val url         = s"${config.baseUrl}/object-store"

    private case class InternalObject(request: Request, contentType: String, lastModifiedInstant: Instant)

    override def putObject[CONTENT](
      path: Path.File,
      content: CONTENT,
      retentionPeriod: RetentionPeriod = config.defaultRetentionPeriod,
      contentType: Option[String] = None,
      owner: String = config.owner
    )(implicit w: ObjectStoreContentWrite[F, CONTENT, Request], hc: HeaderCarrier): F[Unit] =
      M.map(w.writeContent(content, contentType)) { r =>
        val newEntry = Map(
          path.fileName -> InternalObject(
            r,
            contentType.getOrElse("application/octet-stream"),
            Instant.now()
          )
        )
        val directoryUri = path.directory.asUri
        objectStore += directoryUri -> objectStore.get(directoryUri).foldLeft(newEntry)(_ ++ _)
        ()
      }

    override def getObject[CONTENT](path: Path.File, owner: String = config.owner)(implicit
      cr: ObjectStoreContentRead[F, ResBody, CONTENT],
      hc: HeaderCarrier
    ): F[Option[Object[CONTENT]]] = {
      val location = s"$url/object/$owner/${path.asUri}"
      objectStore
        .get(path.directory.asUri)
        .flatMap(_.get(path.fileName))
        .map { internalObject =>
          val wsRequest = internalObject.request.writeBody(wsClient.url("http://foo"))
          val body      = wsRequest.body.asInstanceOf[SourceBody].source.mapMaterializedValue(_ => NotUsed)
          M.map(cr.readContent(body)) { content =>
            Option(
              Object(
                location,
                content,
                ObjectMetadata(
                  internalObject.contentType,
                  internalObject.request.length.getOrElse(0),
                  internalObject.request.md5.getOrElse(""),
                  Instant.now(),
                  Map.empty
                )
              )
            )
          }
        }
        .getOrElse(M.pure(None))
    }

    override def deleteObject(path: Path.File, owner: String = config.owner)(implicit
      hc: HeaderCarrier
    ): F[Unit] = {
      objectStore += path.directory.asUri -> (objectStore.getOrElse(path.directory.asUri, Map.empty) - path.fileName)
      M.pure(())
    }

    override def listObjects(path: Path.Directory, owner: String = config.owner)(implicit
      hc: HeaderCarrier
    ): F[ObjectListing] =
      M.pure(
        ObjectListing(
          objectStore
            .getOrElse(path.asUri, Map.empty)
            .map {
              case (filename, internalObject) =>
                ObjectSummary(
                  path.file(filename),
                  internalObject.request.length.getOrElse(0),
                  internalObject.request.md5.getOrElse(""),
                  internalObject.lastModifiedInstant
                )
            }
            .toList
        )
      )
  }
}
