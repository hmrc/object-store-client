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

import scala.concurrent.{ExecutionContext, Future}

package object either {
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

  val Implicits =
    new PlayObjectStoreContentReads[F]
    with PlayObjectStoreContentWrites[F] {
      override def F(implicit ec: ExecutionContext) = uk.gov.hmrc.objectstore.client.play.either.F

      val InMemoryContentReads = new InMemoryPlayObjectStoreContentReads[F] {
        override def F(implicit ec: ExecutionContext) = uk.gov.hmrc.objectstore.client.play.either.F
      }
  }
}
