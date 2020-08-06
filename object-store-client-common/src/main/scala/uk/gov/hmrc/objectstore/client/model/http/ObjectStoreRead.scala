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


trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

object Functor {

  def apply[F[_]](implicit functor: Functor[F]): Functor[F] = new Functor[F] {
    override def map[A, B](fa: F[A])(f: A => B): F[B] = functor.map(fa)(f)
  }
}

trait ObjectStoreRead[RES, T, F[_]] {
  self =>

  def toObjectListing(response: RES): F[ObjectListing]

  def toObject(response: RES): F[Option[Object[T]]]

  def consume(response: RES): F[Unit]


  def map[B](fn: T => B)(implicit functor: Functor[F]): ObjectStoreRead[RES, B, F] = new ObjectStoreRead[RES, B, F] {
    override def toObjectListing(response: RES): F[ObjectListing] = self.toObjectListing(response)

    override def toObject(response: RES): F[Option[Object[B]]] = Functor[F].map(self.toObject(response))(_.map{ obj =>
      obj.copy(objectContent = fn(obj.objectContent))
    })

    override def consume(response: RES): F[Unit] = self.consume(response)
  }

}

object ObjectStoreReadSyntax {

  implicit class ObjectStoreReadOps[RES, T, F[_]](value: RES) {

    def toObjectListings(implicit r: ObjectStoreRead[RES, T, F]): F[ObjectListing] = r.toObjectListing(value)

    def toObject(implicit r: ObjectStoreRead[RES, T, F]): F[Option[Object[T]]] = r.toObject(value)

    def consume(implicit r: ObjectStoreRead[RES, T, F]): F[Unit] = r.consume(value)

  }
}
