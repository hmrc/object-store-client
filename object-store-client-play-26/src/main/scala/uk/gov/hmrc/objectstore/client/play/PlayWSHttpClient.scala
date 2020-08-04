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
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.model.http.HttpClient
import uk.gov.hmrc.objectstore.client.play.PlayWSHttpClient.{Request, Response}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

case class HttpBody[BODY](length: Option[Long], md5: Option[String], writeBody: BODY, release: () => Unit)

object PlayWSHttpClient {
  type Request  = Future[HttpBody[WSRequest => WSRequest]]
  type Response = Future[WSResponse]
}
class PlayWSHttpClient @Inject()(wsClient: WSClient, config: ObjectStoreClientConfig)(implicit ec: ExecutionContext)
    extends HttpClient[Request, Response] {

  private val logger: Logger = Logger(this.getClass)

  override def put(url: String, body: Request): Future[WSResponse] =
    invoke(
      url             = url,
      method          = "PUT",
      processResponse = identity,
      body            = body
    )

  override def post(url: String, body: Request): Future[WSResponse] = invoke(
    url             = url,
    method          = "POST",
    processResponse = identity,
    body            = body
  )

  override def get(url: String): Future[WSResponse] = invoke(
    url             = url,
    method          = "GET",
    processResponse = identity
  )

  override def delete(url: String): Future[WSResponse] = invoke(
    url             = url,
    method          = "DELETE",
    processResponse = identity
  )

  private val empty = Future.successful(
    HttpBody[WSRequest => WSRequest](
      length    = None,
      md5       = None,
      writeBody = identity,
      release   = () => ()
    ))

  private def invoke[T](
    url: String,
    method: String,
    processResponse: WSResponse => T,
    headers: List[(String, String)]         = List.empty,
    queryParameters: List[(String, String)] = List.empty,
    body: Request                           = empty
  ): Future[T] = {

    logger.info(s"Request: Url: $url")
    body.flatMap { httpBody =>
      val hdrs = (headers ++ httpBody.length.map("Content-Length" -> _.toString) ++ httpBody.md5.map(
        "Content-MD5" -> _)) :+ authorizationHeader

      val request = wsClient
        .url(url)
        .withFollowRedirects(false)
        .withMethod(method)
        .withHttpHeaders(hdrs: _*)
        .withQueryStringParameters(queryParameters: _*)
        .withRequestTimeout(Duration.Inf)

      httpBody
        .writeBody(request)
        .execute(method)
        .map(logResponse)
        .map(processResponse)
        .andThen { case _ => httpBody.release() }
    }
  }

  private def authorizationHeader: (String, String) =
    ("Authorization", config.authorizationToken)

  private def logResponse(response: WSResponse): WSResponse = {
    logger.info(s"Response: Status ${response.status}, Headers ${response.headers}")
    response
  }
}
