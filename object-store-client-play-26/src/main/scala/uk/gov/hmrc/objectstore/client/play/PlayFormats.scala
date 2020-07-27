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

package uk.gov.hmrc.objectstore.client.play

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json.{Format, _}
import uk.gov.hmrc.objectstore.client.model.objectstore.{ObjectListing, ObjectSummary}

object PlayFormats {

  val objectSummaryFormat: Format[ObjectSummary] = {
    ((__ \ "location").format[String]
      ~ (__ \ "contentLength").format[Long]
      ~ (__ \ "contentMD5").format[String]
      ~ (__ \ "lastModified").format[String]
      )(ObjectSummary.apply, unlift(ObjectSummary.unapply))
  }

  val objectListingFormat: Format[ObjectListing] = {
    implicit val osf: Format[ObjectSummary] = objectSummaryFormat
    Format.at[List[ObjectSummary]](__ \ "objects").inmap(ObjectListing.apply, _.objectSummaries)
  }
}
