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

package uk.gov.hmrc.objectstore.client.play.modules

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.objectstore.client.RetentionPeriod

import java.util.UUID.randomUUID

class ObjectStoreClientConfigProviderSpecs extends AnyWordSpec with Matchers {

  "provider" should {
    "provide ObjectStoreClientConfig with base url" in new Setup {
      provider.get().baseUrl shouldBe "https://service:8000"
    }

    "provide ObjectStoreClientConfig with base url when no protocol is provided" in new Setup {
      override val overrideConfig = Map(
        "microservice.services.object-store" -> Map(
          "host"     -> "service",
          "port"     -> 8000
        ),
      )
      provider.get().baseUrl shouldBe "http://service:8000"
    }

    "provide ObjectStoreClientConfig with owner as the appName" in new Setup {
      provider.get().owner shouldBe appName
    }

    "provide ObjectStoreClientConfig with authorizationToken" in new Setup {
      provider.get().authorizationToken shouldBe internalAuthToken
    }

    "provide ObjectStoreClientConfig with defaultRetentionPeriod" in new Setup {
      provider.get().defaultRetentionPeriod shouldBe RetentionPeriod.OneWeek
    }

    "throw IllegalStateException when configured retention period is an invalid value" in new Setup {
      override val overrideConfig = Map(
        "object-store.default-retention-period" -> "1week"
      )
      an [IllegalStateException] should be thrownBy provider.get
    }
  }

  trait Setup {
    val overrideConfig: Map[String, Any] = Map.empty
    val internalAuthToken                = randomUUID().toString
    val appName = "hello-world"
    lazy val provider = new ObjectStoreClientConfigProvider(
      Configuration.from(
        Map(
          "appName" -> appName,
          "microservice.services.object-store" -> Map[String, Any](
            "protocol" -> "https",
            "host"     -> "service",
            "port"     -> 8000
          ),
          "internal-auth.token"                   -> internalAuthToken,
          "object-store.default-retention-period" -> "1-week"
        ) ++ overrideConfig
      )
    )
  }
}
