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

package uk.gov.hmrc.objectstore.client.play

import java.io.InputStream
import java.security.{DigestInputStream, MessageDigest}
import java.util.Base64

object Md5Hash {
  def fromInputStream(is: InputStream): String =
    try {
      val md  = MessageDigest.getInstance("MD5")
      val dis = new DigestInputStream(is, md)
      Iterator.continually(dis.read()).takeWhile(_ != -1).toArray
      Base64.getEncoder().encodeToString(md.digest())
    } finally {
      is.close()
    }
}
