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

trait ObjectStoreRead[RES, T, F[_]] {

  def toObjectListing(response: RES): F[ObjectListing]

  def toObject(response: RES): F[Option[Object[T]]]

  def consume(response: RES): F[Unit]

}

object ObjectStoreReadSyntax {

  implicit class ObjectStoreReadOps[RES, T, F[_]](value: RES) {

    def toObjectListings(implicit r: ObjectStoreRead[RES, T, F]): F[ObjectListing] = r.toObjectListing(value)

    def toObject(implicit r: ObjectStoreRead[RES, T, F]): F[Option[Object[T]]] = r.toObject(value)

    def consume(implicit r: ObjectStoreRead[RES, T, F]): F[Unit] = r.consume(value)
  }
}
