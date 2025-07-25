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

import uk.gov.hmrc.objectstore.client.{PresignedUrlRequest, UrlUploadRequest, ZipRequest}

trait ObjectStoreWrite[F[_], REQ_BODY] {
  private[objectstore] def fromZipRequest(request: ZipRequest): F[REQ_BODY]

  private[objectstore] def fromUrlUploadRequest(request: UrlUploadRequest): F[REQ_BODY]

  private[objectstore] def fromPresignedUrlRequest(req: PresignedUrlRequest): F[REQ_BODY]
}
