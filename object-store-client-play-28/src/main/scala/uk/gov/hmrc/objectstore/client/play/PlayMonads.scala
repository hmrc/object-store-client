/*
 * Copyright 2023 HM Revenue & Customs
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

trait PlayMonads {

  implicit def futureEitherMonad(implicit ec: ExecutionContext): PlayMonad[FutureEither] =
    new PlayMonad[FutureEither] {
      override def pure[A](a: A): FutureEither[A] =
        Future.successful(Right(a))

      override def flatMap[A, B](fa: FutureEither[A])(fn: A => FutureEither[B]): FutureEither[B] =
        fa.flatMap {
          case Right(a) => fn(a)
          case Left(e)  => raiseError(e)
        }

      override def map[A, B](fa: FutureEither[A])(fn: A => B): FutureEither[B] =
        fa.map(_.right.map(fn))

      def raiseError[A](e: Exception): FutureEither[A] =
        Future.successful(Left(e))

      def liftFuture[A](future: Future[A]): FutureEither[A] =
        future.map(Right(_))
    }

  implicit def futureMonad(implicit ec: ExecutionContext): PlayMonad[Future] =
    new PlayMonad[Future] {

      override def pure[A](a: A): Future[A] = Future.successful(a)

      override def flatMap[A, B](fa: Future[A])(fn: A => Future[B]): Future[B] = fa.flatMap(fn)

      override def map[A, B](fa: Future[A])(fn: A => B): Future[B] =
        fa.map(fn)

      override def liftFuture[A](future: Future[A]): Future[A] = future

      override def raiseError[A](e: Exception): Future[A] = Future.failed(e)
    }
}
