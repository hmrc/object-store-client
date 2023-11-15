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

package uk.gov.hmrc.objectstore.client.play

import play.api.Logger
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HttpException}
import uk.gov.hmrc.objectstore.client.http.HttpClient
import java.net.ConnectException
import java.util.concurrent.TimeoutException

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.language.higherKinds

class PlayWSHttpClient[F[_]](wsClient: WSClient)(implicit ec: ExecutionContext, F: PlayMonad[F])
    extends HttpClient[F, Request, Response] {

  private val logger: Logger = Logger(this.getClass)

  override def put(url: String, body: Request, headers: List[(String, String)]): F[Response] =
    invoke(
      url = url,
      method = "PUT",
      body = body,
      headers = headers
    )

  override def post(url: String, body: Request, headers: List[(String, String)]): F[Response] =
    invoke(
      url = url,
      method = "POST",
      body = body,
      headers = headers
    )

  override def get(url: String, headers: List[(String, String)]): F[Response] =
    invoke(
      url = url,
      method = "GET",
      headers = headers
    )

  override def delete(url: String, headers: List[(String, String)]): F[Response] =
    invoke(
      url = url,
      method = "DELETE",
      headers = headers
    )

  private val empty =
    HttpBody[WSRequest => WSRequest](
      length = None,
      md5 = None,
      writeBody = identity,
      release = () => ()
    )

  private def invoke(
    url: String,
    method: String,
    headers: List[(String, String)],
    queryParameters: List[(String, String)] = List.empty,
    body: Request = empty
  ): F[WSResponse] = {

    logger.info(s"Request: Url: $url")
    val hdrs = headers ++
      body.length.map("Content-Length" -> _.toString) ++
      body.md5.map("Content-MD5" -> _.value)

    val wsRequest = wsClient
      .url(url)
      .withFollowRedirects(false)
      .withMethod(method)
      .withHttpHeaders(hdrs: _*)
      .withQueryStringParameters(queryParameters: _*)
      .withRequestTimeout(Duration.Inf)

    val res =
      body
        .writeBody(wsRequest)
        .stream()
        .map(logResponse)
        .map(Right(_): Either[HttpException, WSResponse])
        .recover {
          case e: TimeoutException =>
            Left(new GatewayTimeoutException(s"$method of '$url' timed out with message '${e.getMessage}'"))
          case e: ConnectException =>
            Left(new BadGatewayException(s"$method of '$url' failed. Caused by: '${e.getMessage}'"))
        }
        .andThen { case _ => body.release() }

    F.flatMap(F.liftFuture(res)) {
      case Left(e)  => F.raiseError(e)
      case Right(r) => F.pure(r)
    }
  }

  private def logResponse(response: WSResponse): WSResponse = {
    logger.info(s"Response: Status ${response.status}, Headers ${response.headers}")
    response
  }
}
