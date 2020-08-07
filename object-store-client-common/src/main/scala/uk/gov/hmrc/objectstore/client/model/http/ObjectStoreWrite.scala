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

import uk.gov.hmrc.objectstore.client.model.Monad

case class Payload[CONTENT](length: Long, md5Hash: String, content: CONTENT)

trait ObjectStoreContentWrite[F[_], CONTENT, REQ] { outer =>
  def writeContent(content: CONTENT): F[REQ]

  def contramap[CONTENT2](f: CONTENT2 => CONTENT): ObjectStoreContentWrite[F, CONTENT2, REQ] =
    new ObjectStoreContentWrite[F, CONTENT2, REQ] {
      override def writeContent(content: CONTENT2): F[REQ] =
        outer.writeContent(f(content))
    }

  def contramapF[CONTENT2](f: CONTENT2 => F[CONTENT])(implicit F: Monad[F]): ObjectStoreContentWrite[F, CONTENT2, REQ] =
    new ObjectStoreContentWrite[F, CONTENT2, REQ] {
      override def writeContent(content: CONTENT2): F[REQ] =
        F.flatMap(f(content))(outer.writeContent)
    }
}
