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
import play.api.Logger
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.objectstore.client.model.http.HttpClient
import uk.gov.hmrc.objectstore.client.play.PlayWSHttpClient.{Request, Response}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

// TODO move this into common (and empty implementation) ?
// relationship with Payload?
case class HttpBody[BODY](length: Option[Long], md5: Option[String], writeBody: BODY, release: () => Unit)

object PlayWSHttpClient {
  type Request  = HttpBody[WSRequest => WSRequest]
  type Response = WSResponse
}
class PlayWSHttpClient @Inject()(wsClient: WSClient)(implicit ec: ExecutionContext)
    extends HttpClient[Future, Request, Response] {

  private val logger: Logger = Logger(this.getClass)

  override def put(url: String, body: Request, headers: List[(String, String)]): Future[WSResponse] =
    invoke(
      url             = url,
      method          = "PUT",
      processResponse = identity,
      body            = body,
      headers         = headers
    )

  override def post(url: String, body: Request, headers: List[(String, String)]): Future[WSResponse] = invoke(
    url             = url,
    method          = "POST",
    processResponse = identity,
    body            = body,
    headers         = headers
  )

  override def get(url: String, headers: List[(String, String)]): Future[WSResponse] = invoke(
    url             = url,
    method          = "GET",
    processResponse = identity,
    headers         = headers
  )

  override def delete(url: String, headers: List[(String, String)]): Future[WSResponse] = invoke(
    url             = url,
    method          = "DELETE",
    processResponse = identity,
    headers         = headers
  )

  private val empty =
    HttpBody[WSRequest => WSRequest](
      length    = None,
      md5       = None,
      writeBody = identity,
      release   = () => ()
    )

  private def invoke[T](
    url            : String,
    method         : String,
    processResponse: WSResponse => T,
    headers        : List[(String, String)],
    queryParameters: List[(String, String)] = List.empty,
    body           : Request                = empty
  ): Future[T] = {

    logger.info(s"Request: Url: $url")
    val hdrs = headers ++
      body.length.map("Content-Length" -> _.toString) ++
      body.md5.map("Content-MD5" -> _)

    val wsRequest = wsClient
      .url(url)
      .withFollowRedirects(false)
      .withMethod(method)
      .withHttpHeaders(hdrs: _*)
      .withQueryStringParameters(queryParameters: _*)
      .withRequestTimeout(Duration.Inf)

    body
      .writeBody(wsRequest)
      .execute(method)
      .map(logResponse)
      .map(processResponse)
      .andThen { case _ => body.release() }
  }

  private def logResponse(response: WSResponse): WSResponse = {
    logger.info(s"Response: Status ${response.status}, Headers ${response.headers}")
    response
  }
}
