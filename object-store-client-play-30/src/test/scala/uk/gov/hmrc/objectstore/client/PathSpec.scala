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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PathSpec extends AnyWordSpec with Matchers {
  "Path asUri" must {
    "generate a directory uri correctly with a trailing slash if present" in {
      val rawDirectoryPath = "directory/"
      val directory        = Path.Directory(rawDirectoryPath)
      val uri              = directory.asUri
      assertResult("directory/")(uri)
    }

    "generate a directory uri correctly with a trailing slash if not present" in {
      val rawDirectoryPath = "directory"
      val directory        = Path.Directory(rawDirectoryPath)
      val uri              = directory.asUri
      assertResult("directory/")(uri)
    }

    "generate a directory uri when raw path is empty string" in {
      val rawDirectoryPath = ""
      val directory        = Path.Directory(rawDirectoryPath)
      val uri              = directory.asUri
      assertResult("")(uri)
    }

    "generate a file uri correctly without a trailing slash if present" in {
      val file = Path.Directory("directory").file("file/")
      val uri  = file.asUri
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(uri)
    }

    "generate a file uri correctly without a trailing slash if not present" in {
      val file = Path.Directory("directory").file("file")
      val uri  = file.asUri
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(uri)
    }

    "generate a file uri correctly without a trailing slash if present using apply method" in {
      val file = Path.File("directory/file/")
      val uri  = file.asUri
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(uri)
    }

    "generate a file uri correctly without a trailing slash if not present using apply method" in {
      val file = Path.File("directory/file")
      val uri  = file.asUri
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(uri)
    }
  }
}
