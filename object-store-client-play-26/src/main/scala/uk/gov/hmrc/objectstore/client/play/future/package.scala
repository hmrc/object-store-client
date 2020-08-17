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

package object future {
  type F[A] = Future[A]

  implicit def F(implicit ec: ExecutionContext): PlayMonad[F] =
    new PlayMonad[F] {
      override def pure[A](a: A): Future[A] =
        Future.successful(a)

      override def flatMap[A, B](fa: Future[A])(fn: A => Future[B]): Future[B] =
        fa.flatMap(fn)

      override def map[A, B](fa: Future[A])(fn: A => B): Future[B] =
        fa.map(fn)

      override def raiseError[A](e: PlayObjectStoreException): Future[A] =
        Future.failed(e)

      def liftFuture[A](future: Future[A]): F[A] =
        future
    }

  // TODO can we bring into scope automatically, e.g. import directly into package object (circular reference to F...)
  val Implicits =
    new PlayObjectStoreContentReads[F]
    with PlayObjectStoreContentWrites[F] {
      override def F(implicit ec: ExecutionContext) = uk.gov.hmrc.objectstore.client.play.future.F

      val InMemoryContentReads = new InMemoryPlayObjectStoreContentReads[F] {
        override def F(implicit ec: ExecutionContext) = uk.gov.hmrc.objectstore.client.play.future.F
      }
    }
}
