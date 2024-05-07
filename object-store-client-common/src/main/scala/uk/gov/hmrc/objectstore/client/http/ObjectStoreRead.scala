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

import uk.gov.hmrc.objectstore.client.{Object, ObjectListing, ObjectSummaryWithMd5}

trait ObjectStoreRead[F[_], RES, RES_BODY] { self =>

  def toObjectListing(response: RES): F[ObjectListing]

  def toObjectSummaryWithMd5(response: RES): F[ObjectSummaryWithMd5]

  def toObject(location: String, response: RES): F[Option[Object[RES_BODY]]]

  def consume(response: RES): F[Unit]
}
