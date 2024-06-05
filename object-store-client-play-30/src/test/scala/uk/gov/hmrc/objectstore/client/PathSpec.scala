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
  "Path asUri" should {
    "generate a directory uri correctly with a trailing slash if present" in {
      val rawDirectoryPath = "directory/"
      val directory        = Path.Directory(rawDirectoryPath)
      assertResult("directory/")(directory.asUri)
    }

    "generate a directory uri correctly with a trailing slash if not present" in {
      val rawDirectoryPath = "directory"
      val directory        = Path.Directory(rawDirectoryPath)
      assertResult("directory/")(directory.asUri)
    }

    "generate a directory uri when raw path is empty string" in {
      val rawDirectoryPath = ""
      val directory        = Path.Directory(rawDirectoryPath)
      assertResult("")(directory.asUri)
    }

    "generate a file uri correctly without a trailing slash if present" in {
      val file = Path.Directory("directory").file("file/")
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(file.asUri)
    }

    "generate a file uri correctly with correctly encoded spaces" in {
      val file = Path.Directory("directory").file("file with spaces")
      assertResult("directory/")(file.directory.asUri)
      assertResult("file with spaces")(file.fileName)
      assertResult("directory/file%20with%20spaces")(file.asUri)
    }

    "generate a file uri correctly without a trailing slash if not present" in {
      val file = Path.Directory("directory").file("file")
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(file.asUri)
    }

    "generate a file uri correctly with correct + encoding if present using apply method" in {
      val file = Path.File("directory/file+with+plus")
      assertResult("directory/")(file.directory.asUri)
      assertResult("file+with+plus")(file.fileName)
      assertResult("directory/file%2Bwith%2Bplus")(file.asUri)
    }

    "generate a file uri correctly without a trailing slash if present using apply method" in {
      val file = Path.File("directory/file/")
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(file.asUri)
    }

    "generate a file uri correctly without a trailing slash if not present using apply method" in {
      val file = Path.File("directory/file")
      assertResult("directory/")(file.directory.asUri)
      assertResult("directory/file")(file.asUri)
    }
  }
}
