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

package uk.gov.hmrc.objectstore.client

import java.net.{URLDecoder, URLEncoder}
import java.time.Instant

sealed trait Path { def asUri: String }

object Path {
  case class Directory(value: String) extends Path {
    override val asUri: String = if (value.endsWith("/")) value.dropRight(1) else value
  }

  case class File(directory: Directory, fileName: String) extends Path {
    if (fileName.isEmpty) throw new IllegalArgumentException(s"fileName cannot be empty")

    override val asUri: String =
      s"${directory.asUri}/${URLEncoder.encode(fileName, "UTF-8")}"
  }

  object File {
    def apply(uri: String): Path.File = {
      val (directory, fileName) = uri.splitAt(uri.lastIndexOf("/"))
      Path.File(Path.Directory(directory), URLDecoder.decode(fileName.stripPrefix("/"), "UTF-8"))
    }
  }
}

case class Object[CONTENT](
  location: String,
  content: CONTENT,
  metadata: ObjectMetadata
)

final case class ObjectListing(
  objectSummaries: List[ObjectSummary]
)

final case class ObjectMetadata(
  contentType: String,
  contentLength: Long,
  contentMd5: String,
  lastModified: Instant,
  userMetadata: Map[String, String]
)

final case class ObjectSummary(
  location: Path.File,
  contentLength: Long,
  contentMd5: String,
  lastModified: Instant
)
