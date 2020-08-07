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

import uk.gov.hmrc.objectstore.client.model.{Functor, Monad}
import uk.gov.hmrc.objectstore.client.model.objectstore.{Object, ObjectListing}

import scala.language.higherKinds

trait ObjectStoreRead[F[_], RES] {

  def toObjectListing(response: RES): F[ObjectListing]

  def toObject[CONTENT](response: RES, toContent: RES => F[CONTENT]): F[Option[Object[CONTENT]]]

  def consume(response: RES): F[Unit]
}

trait ObjectStoreContentRead[F[_], RES, CONTENT] { outer =>

  def readContent(response: RES): F[CONTENT]

  def map[CONTENT2](fn: CONTENT => CONTENT2)(implicit F: Functor[F]): ObjectStoreContentRead[F, RES, CONTENT2] =
    new ObjectStoreContentRead[F, RES, CONTENT2] {
      override def readContent(response: RES): F[CONTENT2] =
        F.map(outer.readContent(response))(fn)
    }

  def mapF[CONTENT2](fn: CONTENT => F[CONTENT2])(implicit F: Monad[F]): ObjectStoreContentRead[F, RES, CONTENT2] =
    new ObjectStoreContentRead[F, RES, CONTENT2] {
      override def readContent(response: RES): F[CONTENT2] =
        F.flatMap(outer.readContent(response))(fn)
    }
}
