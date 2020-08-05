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

package uk.gov.hmrc.objectstore.client.model.http

import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}

import scala.language.higherKinds

trait ObjectStoreRead[RES, T, F[_]] { outer =>

  def toObjectListing(response: RES): F[ObjectListing]

  def toObject(response: RES): F[Option[Object[T]]]

  def consume(response: RES): F[Unit]

  // to avoid a dependency on any Functor library, prove F is a monad here...
  def fPure[A](a: A): F[A]
  def fFlatMap[A, B](fa: F[A])(fn: A => F[B]): F[B]

  def map[T2](fn: T => T2): ObjectStoreRead[RES, T2, F] =
    flatMap(a => fPure(fn(a)))

  def flatMap[T2](fn: T => F[T2]): ObjectStoreRead[RES, T2, F] = new ObjectStoreRead[RES, T2, F] {
    override def toObjectListing(response: RES): F[ObjectListing] = outer.toObjectListing(response)

    override def toObject(response: RES): F[Option[Object[T2]]] =
      fFlatMap(outer.toObject(response)){
        case Some(o) => fFlatMap(fn(o.objectContent))(content => fPure(Some(o.copy(objectContent = content))))
        case None    => fPure(None)
      }

    override def consume(response: RES): F[Unit] = outer.consume(response)

    override def fPure[A](a: A): F[A] = outer.fPure(a)
    override def fFlatMap[A, B](fa: F[A])(fn: A => F[B]): F[B] = outer.fFlatMap(fa)(fn)
  }
}

object ObjectStoreReadSyntax {

  implicit class ObjectStoreReadOps[RES, T, F[_]](value: RES) {

    def toObjectListings(implicit r: ObjectStoreRead[RES, T, F]): F[ObjectListing] = r.toObjectListing(value)

    def toObject(implicit r: ObjectStoreRead[RES, T, F]): F[Option[Object[T]]] = r.toObject(value)

    def consume(implicit r: ObjectStoreRead[RES, T, F]): F[Unit] = r.consume(value)
  }
}
