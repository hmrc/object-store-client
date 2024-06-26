/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.objectstore.client.http

import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.category.Monad

import scala.annotation.implicitNotFound

case class Payload[CONTENT](
  length : Long,
  md5Hash: Md5Hash,
  content: CONTENT
)

@implicitNotFound("""No implicits found for ObjectStoreContentWrite[${F}, ${CONTENT}, ${BODY}].
If you are using a Source[ByteString, _], you may be missing an implicit Materializer""")
trait ObjectStoreContentWrite[F[_], CONTENT, BODY] { outer =>
  def writeContent(
    content    : CONTENT,
    contentType: Option[String],
    contentMd5 : Option[Md5Hash]
  ): F[BODY]

  def contramap[CONTENT2](f: CONTENT2 => CONTENT): ObjectStoreContentWrite[F, CONTENT2, BODY] =
    new ObjectStoreContentWrite[F, CONTENT2, BODY] {
      override def writeContent(
        content    : CONTENT2,
        contentType: Option[String],
        contentMd5 : Option[Md5Hash]
      ): F[BODY] =
        outer.writeContent(f(content), contentType, contentMd5)
    }

  def contramapF[CONTENT2](
    f: CONTENT2 => F[CONTENT]
  )(implicit F: Monad[F]): ObjectStoreContentWrite[F, CONTENT2, BODY] =
    new ObjectStoreContentWrite[F, CONTENT2, BODY] {
      override def writeContent(
        content    : CONTENT2,
        contentType: Option[String],
        contentMd5 : Option[Md5Hash]
      ): F[BODY] =
        F.flatMap(f(content))(c => outer.writeContent(c, contentType, contentMd5))
    }
}
