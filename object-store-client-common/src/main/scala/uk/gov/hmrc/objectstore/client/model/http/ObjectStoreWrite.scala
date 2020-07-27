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

  def write(body: BODY): Future[Option[ObjectStoreWrite.ObjectStoreWriteData]]
}

object Empty

object ObjectStoreWrite {
  implicit val emptyWrite = new ObjectStoreWrite[Empty.type] {
    override def write(body: Empty.type): Future[Option[ObjectStoreWriteData]] =
      Future.successful(None)
  }

  case class ObjectStoreWriteData(
    md5Hash      : String,
    contentLength: Long,
    body         : java.io.File, // TODO is there a common denominator? There are 3 flavours - empty, in-memory, and streamed
    cleanup      : Unit => Unit
  )
}
