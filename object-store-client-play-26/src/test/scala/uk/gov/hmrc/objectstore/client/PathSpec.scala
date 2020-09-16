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

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.objectstore.client.utils.DirectoryUtils._

class PathSpec extends WordSpec with Matchers {
  "Path asUri" must {
    "generate a directory uri correctly" in {
      val rawDirectoryPath = "directory"
      val directory   = directoryPath(rawDirectoryPath)
      val uri         = directory.asUri
      assertResult("directory")(uri)
    }

    "remove the trailing slash from the directory path" in {
      val rawDirectoryPath = "directory/"
      val directory   = directoryPath(rawDirectoryPath)
      val uri         = directory.asUri
      assertResult("directory")(uri)
    }

    "generate a file uri correctly" in {
      val file        = filePath(directoryPath("directory"), "file")
      val uri         = file.asUri
      assertResult("directory/file")(uri)
    }
  }


}
