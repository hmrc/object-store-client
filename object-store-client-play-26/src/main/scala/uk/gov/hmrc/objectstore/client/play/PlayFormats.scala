/*
 * Copyright 2021 HM Revenue & Customs
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

import java.net.URL
import java.time.Instant

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Reads, Writes, __}
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectSummary, ObjectListing, ObjectSummaryWithMd5, Path, RetentionPeriod, ZipRequest, _}

object PlayFormats {

  val directoryFormat: Format[Path.Directory] =
    implicitly[Format[String]]
      .inmap(Path.Directory.apply, _.asUri)

  val fileFormat: Format[Path.File] =
    implicitly[Format[String]]
      .inmap(Path.File.apply, _.asUri)

  val objectSummaryWithMd5Reads: Reads[ObjectSummaryWithMd5] =
    ( (__ \ "location"     ).read[String].map(_.stripPrefix("/object-store/object/")).map(Path.File.apply)
    ~ (__ \ "contentLength").read[Long]
    ~ (__ \ "contentMD5"   ).read[String].map(Md5Hash.apply)
    ~ (__ \ "lastModified" ).read[Instant]
    )(ObjectSummaryWithMd5.apply _)

  val objectSummaryReads: Reads[ObjectSummary] =
    ( (__ \ "location"     ).read[String].map(_.stripPrefix("/object-store/object/")).map(Path.File.apply)
    ~ (__ \ "contentLength").read[Long]
    ~ (__ \ "lastModified" ).read[Instant]
    )(ObjectSummary.apply _)

  val objectListingReads: Reads[ObjectListing] = {
    implicit val osf: Reads[ObjectSummary] = objectSummaryReads
    Reads.at[List[ObjectSummary]](__ \ "objectSummaries").map(ObjectListing.apply)
  }

  val zipRequestWrites: Writes[ZipRequest] =
    ( (__ \ "fromLocation"   ).write[Path.Directory](directoryFormat)
    ~ (__ \ "toLocation"     ).write[Path.File](fileFormat)
    ~ (__ \ "retentionPeriod").write[String].contramap[RetentionPeriod](_.value)
    )(unlift(ZipRequest.unapply))

  val urlUploadRequestWrites: Writes[UrlUploadRequest] =
    ( (__ \ "fromUrl"        ).write[String].contramap[URL](_.toString)
    ~ (__ \ "toLocation"     ).write[Path.File](fileFormat)
    ~ (__ \ "retentionPeriod").write[String].contramap[RetentionPeriod](_.value)
    ~ (__ \ "contentType"    ).writeNullable[String]
    ~ (__ \ "contentMd5"     ).writeNullable[String].contramap[Option[Md5Hash]](_.map(_.value))
    )(unlift(UrlUploadRequest.unapply))

}
