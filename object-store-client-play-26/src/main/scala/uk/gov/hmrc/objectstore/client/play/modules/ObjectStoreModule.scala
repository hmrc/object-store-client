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

package uk.gov.hmrc.objectstore.client.play.modules

import com.google.inject.Inject
import com.typesafe.config.Config
import javax.inject.Provider
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.objectstore.client.ObjectRetentionPeriod
import uk.gov.hmrc.objectstore.client.ObjectRetentionPeriod.ObjectRetentionPeriod
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig

class ObjectStoreModule() extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[ObjectStoreClientConfig].toProvider[ObjectStoreClientConfigProvider]
  )
}

private class ObjectStoreClientConfigProvider @Inject()(configuration: Configuration)
    extends Provider[ObjectStoreClientConfig] {

  override def get(): ObjectStoreClientConfig = ObjectStoreClientConfig(
    baseUrl                = getBaseUrl(configuration.underlying),
    owner                  = getOwner(configuration.underlying),
    authorizationToken     = getAuthorizationHeader(configuration.underlying),
    defaultRetentionPeriod = getDefaultRetentionPeriod(configuration.underlying)
  )

  private def getBaseUrl(config: Config): String = {
    val osConfig = config.getConfig("microservice.services.object-store")

    val protocol = if (osConfig.hasPath("protocol")) {
      osConfig.getString("protocol")
    } else "http"

    val host = osConfig.getString("host")
    val port = osConfig.getInt("port")

    s"$protocol://$host:$port"
  }

  private def getOwner(config: Config): String =
    config.getString("appName")

  private def getAuthorizationHeader(config: Config): String =
    config.getString("internal-auth.token")

  private def getDefaultRetentionPeriod(config: Config): ObjectRetentionPeriod =
    ObjectRetentionPeriod.withName(config.getString("object-store.default-retention-period"))
}
