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

import java.io.ByteArrayInputStream

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.objectstore.client.Md5Hash

class Md5HashUtilsSpec extends AnyWordSpec with Matchers {

  "Md5HashUtils.fromInputStream" must {

    "convert inputstream to md5Hash" in {
      Md5HashUtils.fromInputStream(new ByteArrayInputStream("asd".getBytes)) shouldBe Md5Hash("eBVpbsvxyW5olLd5RW0zDg==")
    }

    "convert Bytes to md5Hash" in {
      Md5HashUtils.fromBytes("asd".getBytes) shouldBe Md5Hash("eBVpbsvxyW5olLd5RW0zDg==")
    }
  }
}
