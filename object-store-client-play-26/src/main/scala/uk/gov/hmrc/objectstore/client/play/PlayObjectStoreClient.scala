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

import javax.inject.Inject
import uk.gov.hmrc.objectstore.client.model.Monad
import uk.gov.hmrc.objectstore.client.ObjectStoreClient
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig

class PlayObjectStoreClient @Inject()(
  httpClient: PlayWSHttpClient,
  config    : ObjectStoreClientConfig
)(implicit ec: scala.concurrent.ExecutionContext
) extends ObjectStoreClient(httpClient, config)(Monad.monadForFuture(ec))

object PlayObjectStoreClient {
  object Implicits
    extends PlayObjectStoreReads
       with PlayObjectStoreContentReads
       with PlayObjectStoreContentWrites
}
