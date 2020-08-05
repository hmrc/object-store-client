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

case class Payload[T](length: Long, md5Hash: String, content: T)

trait ObjectStoreWrite[BODY, REQ] { outer =>
  def write(body: BODY): REQ

  // ObjectStoreWrite is a contravariant Functor, for composition.
  def contramap[BODY2](f: BODY2 => BODY): ObjectStoreWrite[BODY2, REQ] =
    new ObjectStoreWrite[BODY2, REQ] {
      override def write(body: BODY2): REQ =
        outer.write(f(body))
    }
}

object ObjectStoreWriteSyntax {

  implicit class ObjectStoreWriteOps[BODY, REQ](value: BODY) {
    def write(implicit w: ObjectStoreWrite[BODY, REQ]): REQ = w.write(value)
  }
}
