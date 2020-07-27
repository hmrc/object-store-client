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

package uk.gov.hmrc.objectstore.client.model.http

import scala.concurrent.Future


trait ObjectStoreWrite[BODY] {

  def write(body: BODY): Future[Option[ObjectStoreWriteData]]
}

object Empty

object ObjectStoreWrite {
  implicit val emptyWrite = new ObjectStoreWrite[Empty.type] {
    override def write(body: Empty.type): Future[Option[ObjectStoreWriteData]] =
      Future.successful(None)
  }
}

case class ObjectStoreWriteData(
  md5Hash      : String,
  contentLength: Long,
  body         : ObjectStoreWriteDataBody,
  cleanup      : Unit => Unit
)

sealed trait ObjectStoreWriteDataBody
object ObjectStoreWriteDataBody {
  case object Empty extends ObjectStoreWriteDataBody
  case class InMemory(getBytes: Array[Byte]) extends ObjectStoreWriteDataBody
  // this is our streamed implementation - assumes writing to a local file to get length, md5Hash
  // TODO this is implementation dependent - what if we already have length, md5Hash?
  // what would a good stream representation be without coupling to Akka etc?
  case class File(getFile: java.io.File) extends ObjectStoreWriteDataBody
}
