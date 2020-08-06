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

package uk.gov.hmrc.objectstore.client.model

trait Functor[F[_]] {
  def map[A, B](fa: F[A])(fn: A => B): F[B]
}

trait Monad[F[_]] extends Functor[F] {
  def pure[A](a: A): F[A]

  def flatMap[A, B](fa: F[A])(fn: A => F[B]): F[B]

  override def map[A, B](fa: F[A])(fn: A => B): F[B] =
    flatMap(fa)(a => pure(fn(a)))
}


/*object Functor {
  def functorForFuture(implicit ec: ExecutionContext): Functor[Future] = new Functor[Future] {
    def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
  }
}*/

object Monad {
  import scala.concurrent.{ExecutionContext, Future}
  // TODO if no instance found, need to fail with message "Are you missing an ExecutionContext in scope..."
  def monadForFuture(implicit ec: ExecutionContext): Monad[Future] = new Monad[Future] {
    def pure[A](a: A) = Future.successful(a)
    def flatMap[A, B](fa: Future[A])(fn: A => Future[B]): Future[B] = fa.flatMap(fn)
  }
}