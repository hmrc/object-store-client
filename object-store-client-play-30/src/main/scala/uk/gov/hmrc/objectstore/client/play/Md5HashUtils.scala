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

package uk.gov.hmrc.objectstore.client.play

import java.io.InputStream
import java.security.{DigestInputStream, MessageDigest}
import java.util.Base64

import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.util.ByteString
import uk.gov.hmrc.objectstore.client.Md5Hash

import scala.concurrent.{ExecutionContext, Future}

object Md5HashUtils {
  def fromInputStream(is: InputStream): Md5Hash =
    try {
      val md  = MessageDigest.getInstance("MD5")
      val dis = new DigestInputStream(is, md)
      Iterator.continually(dis.read()).takeWhile(_ != -1).toArray
      Md5Hash(Base64.getEncoder.encodeToString(md.digest()))
    } finally is.close()

  def fromBytes(bytes: Array[Byte]): Md5Hash =
    Md5Hash(Base64.getEncoder.encodeToString(MessageDigest.getInstance("MD5").digest(bytes)))

  def md5HashSink(implicit ec: ExecutionContext): Sink[ByteString, Future[Md5Hash]] = {
    val md = MessageDigest.getInstance("MD5")
    Sink
      .foreach[ByteString](bs => md.update(bs.toArray))
      .mapMaterializedValue(_.map(_ => Md5Hash(Base64.getEncoder.encodeToString(md.digest()))))
  }
}
