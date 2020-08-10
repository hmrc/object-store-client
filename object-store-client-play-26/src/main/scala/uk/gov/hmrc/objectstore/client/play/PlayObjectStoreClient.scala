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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.objectstore.client.ObjectStoreClient
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.model.NaturalTransformation

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PlayObjectStoreClientEither @Inject()(
  httpClient: PlayWSHttpClient,
  read      : PlayObjectStoreReads,
  config    : ObjectStoreClientConfig
)(implicit ec: ExecutionContext
) extends ObjectStoreClient[F, F, Request, Response](
  httpClient, read, config
)

@Singleton
class PlayObjectStoreClient @Inject()(
  httpClient: PlayWSHttpClient,
  read      : PlayObjectStoreReads,
  config    : ObjectStoreClientConfig
)(implicit ec: ExecutionContext
) extends ObjectStoreClient[F, Future, Request, Response](
  httpClient, read, config
)(implicitly, PlayObjectStoreClient.fToFuture)

object PlayObjectStoreClient {
  private def fToFuture(implicit ec: ExecutionContext): NaturalTransformation[F, Future] =
    new NaturalTransformation[F, Future] {
      override def transform[A](f: F[A]): Future[A] =
        f.flatMap {
          case Right(a) => Future.successful(a)
          case Left(e)  => Future.failed(e)
        }
    }

}
