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

import play.api.Logger
import play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.http
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HeaderNames, HeaderCarrier, HttpException, StringContextOps}
import uk.gov.hmrc.http.play.HttpClient2
import uk.gov.hmrc.objectstore.client.http.HttpClient

import java.net.ConnectException
import java.util.concurrent.TimeoutException
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class PlayWSHttpClient[F[_]](httpClient2: HttpClient2)(implicit ec: ExecutionContext, F: PlayMonad[F])
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

    // TODO need to add http-verbs as a dependency to object-sore-client-common to propogate from client (through interface)
    // for now, try to reconstruct for auditing
    implicit val hc: HeaderCarrier = {
      def extractHeader(name: String): Option[String] =
        headers.collectFirst { case (k, v) if k.equalsIgnoreCase(name) => v }
      HeaderCarrier(
        authorization    = extractHeader(HeaderNames.authorisation).map(http.Authorization.apply),
        forwarded        = extractHeader(HeaderNames.xForwardedFor).map(http.ForwardedFor.apply),
        sessionId        = extractHeader(HeaderNames.xSessionId   ).map(http.SessionId.apply),
        requestId        = extractHeader(HeaderNames.xRequestId   ).map(http.RequestId.apply),
        requestChain     = extractHeader(HeaderNames.xRequestChain).fold(http.RequestChain.init)(http.RequestChain.apply),
        nsStamp          = System.nanoTime(),
        extraHeaders     = Seq.empty,
        trueClientIp     = extractHeader(HeaderNames.trueClientIp),
        trueClientPort   = extractHeader(HeaderNames.trueClientPort),
        gaToken          = extractHeader(HeaderNames.googleAnalyticTokenId),
        gaUserId         = extractHeader(HeaderNames.googleAnalyticUserId),
        deviceID         = extractHeader(HeaderNames.deviceID),
        akamaiReputation = extractHeader(HeaderNames.akamaiReputation).map(http.AkamaiReputation.apply),
        otherHeaders     = Seq.empty
      )
    }

    logger.info(s"Request: Url: $url")
    val hdrs = headers ++
      body.length.map("Content-Length" -> _.toString) ++
      body.md5.map("Content-MD5" -> _.value)

    val requestBuilder =
      (method match {
        case "PUT"    => httpClient2.put(url"$url")
        case "POST"   => httpClient2.post(url"$url")
        case "GET"    => httpClient2.get(url"$url")
        case "DELETE" => httpClient2.delete(url"$url")
      }).transform(wsRequest =>
        body.writeBody(
          wsRequest
           .withFollowRedirects(false)
           .withHttpHeaders(hdrs: _*)
           .withQueryStringParameters(queryParameters: _*)
           .withRequestTimeout(Duration.Inf)
        )
      )

    def right(req: WSRequest, responseF: scala.concurrent.Future[WSResponse]): scala.concurrent.Future[WSResponse] = responseF

    val res =
      requestBuilder
        .stream[WSResponse](right)
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

}
