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
import play.api.libs.ws.{EmptyBody, WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.objectstore.client.model.http.{HttpClient, Empty, ObjectStoreWrite, ObjectStoreWriteDataBody}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class PlayWSHttpClient @Inject()(wsClient: WSClient)(implicit ec: ExecutionContext) extends HttpClient[Future[WSResponse]] {

  private val logger: Logger = Logger(this.getClass)

  override def put[BODY: ObjectStoreWrite](url: String, body: BODY): Future[WSResponse] =
    invoke(
      url = url,
      method = "PUT",
      processResponse = identity,
      body = body
    )

  override def post[BODY : ObjectStoreWrite](url: String, body: BODY): Future[WSResponse] = invoke(
    url = url,
    method = "POST",
    processResponse = identity,
    body = body
  )

  override def get(url: String): Future[WSResponse] = invoke(
    url = url,
    method = "GET",
    processResponse = identity,
    body = Empty
  )

  override def delete(url: String): Future[WSResponse] = invoke(
    url = url,
    method = "DELETE",
    processResponse = identity,
    body = Empty
  )

  private def invoke[BODY : ObjectStoreWrite, T](
                         url: String,
                         method: String,
                         processResponse: WSResponse => T,
                         headers: List[(String, String)] = List.empty,
                         queryParameters: List[(String, String)] = List.empty,
                         body: BODY,
                       ): Future[T] = {

    logger.info(s"Request: Url: $url")
    val write = implicitly[ObjectStoreWrite[BODY]]
    write.write(body)
      .flatMap { optData =>
        val md5Hash = optData.map(_.md5Hash) // TODO do something with this
        val hdrs = optData.foldLeft(headers)((headers, data) => headers ++ List("Content-Length" -> data.contentLength.toString))

        val request = wsClient
          .url(url)
          .withFollowRedirects(false)
          .withMethod(method)
          .withHttpHeaders(hdrs: _*)
          .withQueryStringParameters(queryParameters: _*)
          .withRequestTimeout(Duration.Inf)

        def writeBody(request: WSRequest, body: ObjectStoreWriteDataBody) = body match {
          case ObjectStoreWriteDataBody.Empty           => request.withBody(EmptyBody)
          case ObjectStoreWriteDataBody.InMemory(bytes) => request.withBody(bytes)
          case ObjectStoreWriteDataBody.File(file)      => request.withBody(file)
        }

        optData.fold(request.withBody(EmptyBody))(data => writeBody(request, data.body))
          .execute(method)
          .map(logResponse)
          .map(processResponse)
          .andThen { case _ => optData.map(_.cleanup(())) }
      }
  }

  private def logResponse(response: WSResponse): WSResponse = {
    logger.info(s"Response: Status ${response.status}, Headers ${response.headers}")
    response
  }
}