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

package uk.gov.hmrc.objectstore.client

import java.net.{URL, URLDecoder, URLEncoder}
import java.time.Instant

sealed trait Path { def asUri: String }

object Path {
  case class Directory(value: String) extends Path {
    override val asUri: String = {
      val uri = value.stripPrefix("/")
      if (uri.nonEmpty && !uri.endsWith("/"))
        uri + "/"
      else uri
    }

    def file(fileName: String): File = File(this, fileName)
  }

  case class File(directory: Directory, fileName: String) extends Path {
    if (fileName.isEmpty) throw new IllegalArgumentException(s"fileName cannot be empty")

    override val asUri: String =
      s"${directory.asUri}${URLEncoder.encode(fileName.stripSuffix("/"), "UTF-8")}"
  }

  object File {
    def apply(uri: String): Path.File = {
      val (directory, fileName) = uri.splitAt(uri.stripSuffix("/").lastIndexOf("/"))
      Path.File(Path.Directory(directory), URLDecoder.decode(fileName.stripPrefix("/"), "UTF-8"))
    }
  }
}

case class Md5Hash(value: String) extends AnyVal

case class Object[CONTENT](
  location: Path.File,
  content : CONTENT,
  metadata: ObjectMetadata
)

final case class ObjectMetadata(
  contentType  : String,
  contentLength: Long,
  contentMd5   : Md5Hash,
  lastModified : Instant,
  userMetadata : Map[String, String]
)

final case class ObjectListing(
  objectSummaries: List[ObjectSummary]
)

final case class ObjectSummary(
  location     : Path.File,
  contentLength: Long,
  lastModified : Instant
)

final case class ObjectSummaryWithMd5(
  location     : Path.File,
  contentLength: Long,
  contentMd5   : Md5Hash,
  lastModified : Instant
)

sealed abstract class RetentionPeriod(val value: String)

object RetentionPeriod {
  case object OneDay     extends RetentionPeriod("1-day")
  case object OneWeek    extends RetentionPeriod("1-week")
  case object OneMonth   extends RetentionPeriod("1-month")
  case object SixMonths  extends RetentionPeriod("6-months")
  case object OneYear    extends RetentionPeriod("1-year")
  case object SevenYears extends RetentionPeriod("7-years")
  case object TenYears   extends RetentionPeriod("10-years")

  private val allValues: Set[RetentionPeriod] =
    Set(OneDay, OneWeek, OneMonth, SixMonths, OneYear, SevenYears, TenYears)

  def parse(value: String): Either[String, RetentionPeriod] =
    allValues
      .find(_.value == value)
      .toRight(s"Invalid object-store retention period. Valid values - [${allValues.map(_.value).mkString(", ")}]")
}

private[objectstore] final case class ZipRequest(
  from           : Path.Directory,
  to             : Path.File,
  retentionPeriod: RetentionPeriod
)

private[objectstore] final case class UrlUploadRequest(
  fromUrl        : URL,
  toLocation     : Path.File,
  retentionPeriod: RetentionPeriod,
  contentType    : Option[String],
  contentMd5     : Option[Md5Hash]
)
