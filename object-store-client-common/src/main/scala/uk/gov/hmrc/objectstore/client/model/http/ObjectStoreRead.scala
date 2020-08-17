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

import scala.annotation.implicitNotFound
import scala.language.higherKinds

@implicitNotFound("""No implicits found for ObjectStoreRead[${F}, ${RES}].
If you are using Future, you may be missing an implicit ExecutionContext""")
trait ObjectStoreRead[F[_], RES, CONTENT] {

  def toObjectListing(response: RES): F[ObjectListing]

  def toObject(response: RES): F[Option[Object[CONTENT]]] //todo might be a better way to represent this

  def consume(response: RES): F[Unit]
}

trait ObjectStoreContentRead[F[_], HTTP_RES, CONTENT] { outer =>

  def readContent(response: HTTP_RES): F[CONTENT] // todo CF can this just be CONTENT?

  def map[CONTENT2](fn: CONTENT => CONTENT2)(implicit F: Functor[F]): ObjectStoreContentRead[F, HTTP_RES, CONTENT2] =
    new ObjectStoreContentRead[F, HTTP_RES, CONTENT2] {
      override def readContent(response: HTTP_RES): F[CONTENT2] =
        F.map(outer.readContent(response))(fn)
    }

  def mapF[CONTENT2](fn: CONTENT => F[CONTENT2])(implicit F: Monad[F]): ObjectStoreContentRead[F, HTTP_RES, CONTENT2] =
    new ObjectStoreContentRead[F, HTTP_RES, CONTENT2] {
      override def readContent(response: HTTP_RES): F[CONTENT2] =
        F.flatMap(outer.readContent(response))(fn)
    }
}
