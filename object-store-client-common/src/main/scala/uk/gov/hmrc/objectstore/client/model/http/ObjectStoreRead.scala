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


trait ObjectStoreRead[HttpResponse, T, F[_]] {

  def toObjectListing(response: HttpResponse): F[ObjectListing]

  def toObject(response: HttpResponse): F[Option[Object[T]]]

  def consume(response: HttpResponse): F[Unit]

}

object ObjectStoreReadSyntax {

  implicit class ObjectStoreReadOps[HttpResponse, T, F[_]](value: HttpResponse) {

    def toObjectListings(implicit r: ObjectStoreRead[HttpResponse, T, F]): F[ObjectListing] = r.toObjectListing(value)

    def toObject(implicit r: ObjectStoreRead[HttpResponse, T, F]): F[Option[Object[T]]] = r.toObject(value)

    def consume(implicit r: ObjectStoreRead[HttpResponse, T, F]): F[Unit] = r.consume(value)
  }
}
