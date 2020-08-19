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

import java.time.Instant

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}
import uk.gov.hmrc.objectstore.client.model.objectstore.{ObjectListing, ObjectSummary}

object PlayFormats {

  val objectSummaryRead: Reads[ObjectSummary] =
    ( (__ \ "location"     ).read[String].map(_.stripPrefix("/object-store/object/"))
    ~ (__ \ "contentType"  ).read[String]
    ~ (__ \ "contentLength").read[Long]
    ~ (__ \ "contentMD5"   ).read[String]
    ~ (__ \ "lastModified" ).read[Instant]
    )(ObjectSummary.apply _)

  val objectListingRead: Reads[ObjectListing] = {
    implicit val osf: Reads[ObjectSummary] = objectSummaryRead
    Reads.at[List[ObjectSummary]](__ \ "objects").map(ObjectListing.apply _)
  }
}
