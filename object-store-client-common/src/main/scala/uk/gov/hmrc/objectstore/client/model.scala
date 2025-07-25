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
import java.util.Base64

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

    // URLEncoder is only suitable for query parameters. For path parameters we have to ensure correct treatment of `+`
    override val asUri: String =
      s"${directory.asUri}${URLEncoder.encode(fileName.stripSuffix("/"), "UTF-8").replace("+","%20")}"
  }

  object File {
    def apply(uri: String): Path.File = {
      val (directory, fileName) = uri.splitAt(uri.stripSuffix("/").lastIndexOf("/"))
      Path.File(Path.Directory(directory), URLDecoder.decode(fileName.stripPrefix("/").replace("+", "%2B"), "UTF-8"))
    }
  }
}

case class Md5Hash(value: String) extends AnyVal
case class Sha256Checksum(value: String) extends AnyVal

object Sha256Checksum {
  def fromBase64(base64: String): Sha256Checksum = {
    require(isBase64Encoded(base64), s"Invalid Base64 format: $base64")
    Sha256Checksum(base64)
  }

  def fromHex(hex: String): Sha256Checksum = {
    require(isHexEncoded(hex), s"Invalid hex format: $hex")
    Sha256Checksum(hexToBase64(hex))
  }

  private def hexToBase64(hex: String): String = {
    val bytes = hex.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
    Base64.getEncoder.encodeToString(bytes)
  }

  private def isHexEncoded(in: String): Boolean =
    // SHA-256 hex is always 64 hexadecimal characters
    in.matches("^[0-9a-fA-F]{64}$")

  private def isBase64Encoded(in: String): Boolean =
    // SHA-256 Base64 is always 44 characters (43 + 1 padding '=')
    in.matches("^[A-Za-z0-9+/]{43}=$")
}

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

final case class PresignedDownloadUrl(
  downloadUrl  : URL,
  contentLength: Long,
  contentMd5   : Md5Hash
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
  contentMd5     : Option[Md5Hash],
  contentSha256  : Option[Sha256Checksum]
)

private[objectstore] final case class PresignedUrlRequest(
  location: Path.File
)
