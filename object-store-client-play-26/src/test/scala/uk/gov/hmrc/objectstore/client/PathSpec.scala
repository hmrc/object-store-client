package uk.gov.hmrc.objectstore.client

import org.scalatest.{Matchers, WordSpec}

class PathSpec extends WordSpec with Matchers {
  "Path asUri" must {
    "generate a directory uri correctly" in {
      val expectedUri = "directory/"
      val directory   = directoryPath()
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
