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

import javax.inject.{Inject, Singleton}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.objectstore.client.ObjectStoreClient
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.play.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/** Client which returns responses within Future[Either[PlayObjectStoreException, *]]. */
@Singleton
class PlayObjectStoreClientEither @Inject()(
  wsClient: WSClient,
  config  : ObjectStoreClientConfig
)(implicit ec: ExecutionContext
) extends ObjectStoreClient[FutureEither, Request, Response, ResBody](
  new PlayWSHttpClient[FutureEither](wsClient),
  PlayObjectStoreReads.futureEitherReads, config
)

/** Client which returns responses within Future.
  * To handle the client exceptions, you can recover the [[PlayObjectStoreException]]
  */
@Singleton
class PlayObjectStoreClient @Inject()(
  wsClient: WSClient,
  config  : ObjectStoreClientConfig
)(implicit ec: ExecutionContext
) extends ObjectStoreClient[Future, Request, Response, ResBody](
  new PlayWSHttpClient[Future](wsClient),
  PlayObjectStoreReads.futureReads, config
)
