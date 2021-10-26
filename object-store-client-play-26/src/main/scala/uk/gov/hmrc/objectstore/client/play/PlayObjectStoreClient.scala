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

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.play.HttpClient2
import uk.gov.hmrc.objectstore.client.ObjectStoreClient
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.play.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/** Client which returns responses within Future[Either[PlayObjectStoreException, *]]. */
@Singleton
class PlayObjectStoreClientEither @Inject() (
  httpClient2: HttpClient2,
  config: ObjectStoreClientConfig
)(implicit m: Materializer, ec: ExecutionContext)
    extends ObjectStoreClient[FutureEither, Request, Response, ResBody](
      new PlayWSHttpClient[FutureEither](httpClient2),
      PlayObjectStoreReads.futureEitherReads,
      PlayObjectStoreWrites.write,
      config
    )

/** Client which returns responses within Future.
  */
@Singleton
class PlayObjectStoreClient @Inject() (
  httpClient2: HttpClient2,
  config: ObjectStoreClientConfig
)(implicit m: Materializer, ec: ExecutionContext)
    extends ObjectStoreClient[Future, Request, Response, ResBody](
      new PlayWSHttpClient[Future](httpClient2),
      PlayObjectStoreReads.futureReads,
      PlayObjectStoreWrites.write,
      config
    )
