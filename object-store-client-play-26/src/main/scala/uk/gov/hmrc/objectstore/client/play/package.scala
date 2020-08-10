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

package uk.gov.hmrc.objectstore.client

import _root_.play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.objectstore.client.model.{Monad, MonadError}

import scala.concurrent.{ExecutionContext, Future}

package object play {
  type Request  = HttpBody[WSRequest => WSRequest]
  type Response = WSResponse

  type F[A] = Future[Either[PlayObjectStoreException, A]]

  implicit def F(implicit ec: ExecutionContext): PlayMonad[F] =
    new PlayMonad[F] {
      override def pure[A](a: A): F[A] =
        Future.successful(Right(a))

      override def flatMap[A, B](fa: F[A])(fn: A => F[B]): F[B] =
        fa.flatMap {
          case Right(a) => fn(a)
          case Left(e)  => raiseError(e)
        }

      override def map[A, B](fa: F[A])(fn: A => B): F[B] =
        fa.map(_.right.map(fn))

      def raiseError[A](e: PlayObjectStoreException): F[A] =
        Future.successful(Left(e))

      def liftFuture[A](future: Future[A]): F[A] =
        future.map(Right(_))
    }

  object Implicits
    extends PlayObjectStoreContentReads
       with PlayObjectStoreContentWrites {

      object InMemoryReads extends InMemoryPlayObjectStoreContentReads
  }
}

package play {
  abstract class PlayObjectStoreException(message: String) extends Exception(message)
  case class UpstreamErrorResponse(message: String, statusCode: Int) extends PlayObjectStoreException(message)
  case class OtherError(message: String) extends PlayObjectStoreException(message)

  // TODO move this into common (and empty implementation) ?
  // relationship with Payload?
  case class HttpBody[BODY](length: Option[Long], md5: Option[String], writeBody: BODY, release: () => Unit)

  // the play implementation operates over Future - this allows us to embed any Future into the operating F
  // without requiring a full Monad Transformer stack
  trait MonadFuture[F[_]] extends Monad[F] {
    def liftFuture[A](future: Future[A]): F[A]
  }

  // simplify by unifying the type classes required for the Play implementations
  trait PlayMonad[F[_]] extends MonadError[F, PlayObjectStoreException] with MonadFuture[F]
}
