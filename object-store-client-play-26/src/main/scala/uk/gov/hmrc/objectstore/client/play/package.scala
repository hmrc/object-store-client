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

package uk.gov.hmrc.objectstore.client

import _root_.play.api.libs.ws.{WSRequest, WSResponse}
import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import uk.gov.hmrc.objectstore.client.category.{Monad, MonadError}

import scala.concurrent.Future

package object play {
  type Request  = HttpBody[WSRequest => WSRequest]
  type Response = WSResponse
  type ResBody  = Source[ByteString, NotUsed]

  type FutureEither[A] = Future[Either[Exception, A]]

  object Implicits extends PlayObjectStoreContentReads with PlayObjectStoreContentWrites with PlayMonads {

    object InMemoryReads extends InMemoryPlayObjectStoreContentReads
  }
}

package play {

  // TODO move this into common (and empty implementation) ?
  // relationship with Payload?
  case class HttpBody[BODY](
    length: Option[Long],
    md5: Option[String],
    writeBody: BODY,
    release: () => Unit
  )

  // the play implementation operates over Future - this allows us to embed any Future into the operating F
  // without requiring a full Monad Transformer stack
  trait MonadFuture[F[_]] extends Monad[F] {
    def liftFuture[A](future: Future[A]): F[A]
  }

  // simplify by unifying the type classes required for the Play implementations
  trait PlayMonad[F[_]] extends MonadError[F, Exception] with MonadFuture[F]
}
