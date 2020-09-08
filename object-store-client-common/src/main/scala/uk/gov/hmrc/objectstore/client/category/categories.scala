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

package uk.gov.hmrc.objectstore.client.category

import scala.annotation.implicitNotFound
import scala.language.higherKinds

@implicitNotFound("""Cannot find an implicit Functor[${F}].
If you are using Future, you may be missing an implicit ExecutionContext.""")
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(fn: A => B): F[B]
}

@implicitNotFound("""Cannot find an implicit Monad[${F}].
If you are using Future, you may be missing an implicit ExecutionContext.""")
trait Monad[F[_]] extends Functor[F] {
  def pure[A](a: A): F[A]

  def flatMap[A, B](fa: F[A])(fn: A => F[B]): F[B]

  override def map[A, B](fa: F[A])(fn: A => B): F[B] =
    flatMap(fa)(a => pure(fn(a)))
}

trait MonadError[F[_], E] extends Monad[F] {
  def raiseError[A](e: E): F[A]
}

object Monad {
  implicit def monadForMonadError[F[_], E](implicit F: MonadError[F, E]): Monad[F] = F
}

object Functor {
  implicit def functorForMonad[F[_]](implicit F: Monad[F]): Functor[F] = F
}
