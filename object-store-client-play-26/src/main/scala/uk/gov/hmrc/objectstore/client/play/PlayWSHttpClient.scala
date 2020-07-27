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

import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import javax.inject.Inject
import play.api.Logger
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.objectstore.client.model.http.HttpClient

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class PlayWSHttpClient @Inject()(wsClient: WSClient)(implicit ec: ExecutionContext, m: Materializer) extends HttpClient[Source[ByteString, _], Future[WSResponse]] {

  private val logger: Logger = Logger(this.getClass)

  override def put(url: String, body: Source[ByteString, _]): Future[WSResponse] = {
    invoke(
      url = url,
      method = "PUT",
      processResponse = identity,
      body = body
    )
  }

  override def post(url: String, body: Source[ByteString, _]): Future[WSResponse] = invoke(
    url = url,
    method = "POST",
    processResponse = identity,
    body = body
  )

  override def get(url: String): Future[WSResponse] = invoke(
    url = url,
    method = "GET",
    processResponse = identity,
  )

  override def delete(url: String): Future[WSResponse] = invoke(
    url = url,
    method = "DELETE",
    processResponse = identity,
  )

  private def invoke[T](
                         url: String,
                         method: String,
                         processResponse: WSResponse => T,
                         headers: List[(String, String)] = List.empty,
                         queryParameters: List[(String, String)] = List.empty,
                         body: Source[ByteString, _] = Source.empty,
                       ): Future[T] = {

    logger.info(s"Request: Url: $url")
    val path = SingletonTemporaryFileCreator.create().path
    val result: Future[IOResult] = body.runWith(FileIO.toPath(path))

    result.flatMap { _ =>

      //todo just do this when there is no content length

      val file = path.toFile
      val hdrs = headers ++ List(("Content-Length", file.length().toString))

      wsClient
        .url(url)
        .withFollowRedirects(false)
        .withMethod(method)
        .withHttpHeaders(hdrs: _*)
        .withQueryStringParameters(queryParameters: _*)
        .withRequestTimeout(Duration.Inf)
        .withBody(file)
        .execute(method)
        .map(logResponse)
        .map(processResponse)
    }

    //todo consider file cleanup
  }

  private def logResponse(response: WSResponse): WSResponse = {
    logger.info(s"Response: Status ${response.status}, Headers ${response.headers}")
    response
  }
}