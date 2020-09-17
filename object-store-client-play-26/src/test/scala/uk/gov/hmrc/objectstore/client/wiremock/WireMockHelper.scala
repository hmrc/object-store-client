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

package uk.gov.hmrc.objectstore.client.wiremock

import java.net.ServerSocket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.util.{Random, Try}

trait WireMockHelper extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  val wireMockPort = PortTester.findPort()
  val wireMockHost = "localhost"
  val wireMockUrl  = s"http://$wireMockHost:$wireMockPort"

  private val wmConfig       = wireMockConfig().port(wireMockPort)
  private val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    configureFor(wireMockHost, wireMockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = reset()

  override protected def beforeAll: Unit = {
    wireMockServer.start()
    configureFor("localhost", wireMockPort)
  }

  override protected def afterAll: Unit =
    wireMockServer.stop()

  override protected def afterEach: Unit =
    reset()

}

private object PortTester {

  def findPort(excluded: Int*): Int =
    Random.shuffle((6001 to 7000).toVector).find(port => !excluded.contains(port) && isFree(port)).getOrElse(throw new Exception("No free port"))

  private def isFree(port: Int): Boolean = {
    val triedSocket = Try {
      val serverSocket = new ServerSocket(port)
      Try(serverSocket.close())
      serverSocket
    }
    triedSocket.isSuccess
  }
}
