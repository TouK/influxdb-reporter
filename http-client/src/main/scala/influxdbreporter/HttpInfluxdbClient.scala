/*
 * Copyright 2015
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
package influxdbreporter

import com.typesafe.scalalogging.LazyLogging
import influxdbreporter.core.MetricClient
import influxdbreporter.core.writers.WriterData
import org.asynchttpclient._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.language.postfixOps

class HttpInfluxdbClient(connectionData: ConnectionData)
                        (implicit executionContext: ExecutionContext, requestTimeout: FiniteDuration)
  extends MetricClient[String] with LazyLogging {

  private val MaxConnections = 5
  private val LoadEncoding = "UTF-8"
  private val InfluxSuccessStatusCode = 204

  private lazy val httpClient = new AsyncHttpClientWrapper(
    new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
      .setMaxConnectionsPerHost(MaxConnections)
      .setMaxConnections(MaxConnections)
      .setRequestTimeout(requestTimeout.toMillis.toInt)
      .build()
    ))

  private val influxdbWriteRequestBuilder = httpClient.underlying
    .preparePost(s"http://${connectionData.address}:${connectionData.port}/write")
    .addQueryParam("db", connectionData.dbName)
    .addQueryParam("u", connectionData.user)
    .addQueryParam("p", connectionData.password)

  override def sendData(writerData: List[WriterData[String]]): Future[Boolean] = {
    val influxData = writerData map (_.data) mkString
    val request = influxdbWriteRequestBuilder.setBody(influxData.getBytes(LoadEncoding)).build()
    logRequestResponse(request) {
      httpClient.send(request)
    } map isResponseSucceed
  }

  private def logRequestResponse(request: Request): (Future[Response] => Future[Response]) = result => {
    def requestBodyToString(req: Request) = new String(req.getByteData, LoadEncoding)

    result onComplete {
      case Success(response) if isResponseSucceed(response) =>
        logger.debug(s"Data was sent and successfully written")
      case Success(response) =>
        logger.warn(s"Request: $request\nInfluxdb cannot handle request with metrics: status=[${response.getStatusCode}]")
        logger.debug(s"Request body:\\n${requestBodyToString(request)}")
      case Failure(ex) =>
        logger.error(s"Request: $request\nInfluxdb cannot handle request with metrics:", ex)
        logger.debug(s"Request body:\\n${requestBodyToString(request)}")
    }
    result
  }

  private def isResponseSucceed(response: Response) = response.getStatusCode == InfluxSuccessStatusCode
}

case class ConnectionData(address: String, port: Int, dbName: String, user: String, password: String)