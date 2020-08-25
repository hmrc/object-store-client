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

import uk.gov.hmrc.objectstore.client.model.objectstore.Object
import uk.gov.hmrc.objectstore.client.model.{Functor, Monad}

import scala.language.higherKinds

trait ObjectStoreContentRead[F[_], RES_BODY, CONTENT] { outer =>

  def readContent(response: F[Option[Object[RES_BODY]]]): F[Option[Object[CONTENT]]]

  def map[CONTENT2](fn: CONTENT => CONTENT2)(implicit F: Functor[F]): ObjectStoreContentRead[F, RES_BODY, CONTENT2] =
    new ObjectStoreContentRead[F, RES_BODY, CONTENT2] {
      override def readContent(response: F[Option[Object[RES_BODY]]]): F[Option[Object[CONTENT2]]] = {
        F.map(outer.readContent(response))(_.map(obj => obj.copy(content = fn(obj.content))))
      }
    }

  def mapF[CONTENT2](fn: CONTENT => F[CONTENT2])(implicit F: Monad[F]): ObjectStoreContentRead[F, RES_BODY, CONTENT2] =
    new ObjectStoreContentRead[F, RES_BODY, CONTENT2] {
      override def readContent(response: F[Option[Object[RES_BODY]]]): F[Option[Object[CONTENT2]]] =
        F.flatMap(outer.readContent(response)) {
          case Some(obj) => F.map(fn(obj.content))(content => Some(obj.copy(content = content)))
          case _ => F.pure(None)
        }
    }
}
