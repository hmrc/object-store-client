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

package uk.gov.hmrc.objectstore.client.play

import play.api.libs.ws.{WSRequest, writeableOf_JsValue}
import uk.gov.hmrc.objectstore.client.{UrlUploadRequest, ZipRequest}
import uk.gov.hmrc.objectstore.client.http.ObjectStoreWrite

object PlayObjectStoreWrites {
  implicit def write[F[_]](implicit F: PlayMonad[F]): ObjectStoreWrite[F, Request] =
    new ObjectStoreWrite[F, Request] {
      def fromZipRequest(request: ZipRequest): F[Request] =
        F.pure(
          HttpBody(
            length    = None,
            md5       = None,
            writeBody = (req: WSRequest) => req.withBody(PlayFormats.zipRequestWrites.writes(request)),
            release   = () => ()
          )
        )

      override def fromUrlUploadRequest(request: UrlUploadRequest): F[Request] =
        F.pure(
          HttpBody(
            length    = None,
            md5       = None,
            writeBody = (req: WSRequest) => req.withBody(PlayFormats.urlUploadRequestWrites.writes(request)),
            release   = () => ()
          )
        )
    }
}
