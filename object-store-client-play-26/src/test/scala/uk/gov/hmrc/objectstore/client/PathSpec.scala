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

class PathSpec extends WordSpec with Matchers {
  "Path asUri" must {
    "generate a directory uri correctly" in {
      val expectedUri = "directory"
      val directory   = directoryPath()
      val uri         = directory.asUri
      assertResult(expectedUri)(uri)
    }

    "drop trailing slash in directory uri" in {
      val expectedUri = "directory"
      val directory   = Path.Directory("directory/")
      val uri         = directory.asUri
      assertResult(expectedUri)(uri)
    }

    "generate a file uri correctly" in {
      val expectedUri = "directory/file"
      val file        = filePath()
      val uri         = file.asUri
      assertResult(expectedUri)(uri)
    }
  }

  private def directoryPath(): Path.Directory =
    Path.Directory("directory")

  private def filePath(): Path.File =
    Path.File(directoryPath(), "file")

}
