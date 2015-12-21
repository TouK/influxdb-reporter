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

import com.ning.http.client.Response
import com.typesafe.scalalogging.slf4j.LazyLogging
import dispatch.{Http, Req, url}
import influxdbreporter.core.{MetricClient, WriterData}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class HttpInfluxdbClient(connectionData: ConnectionData)
                        (implicit executionContext: ExecutionContext, requestTimeout: FiniteDuration)
  extends MetricClient[String] with LazyLogging {

  private val InfluxSuccessStatusCode = 204
  private val influxdbWriteUrl = url(s"http://${connectionData.address}:${connectionData.port}/write")
    .addQueryParameter("db", connectionData.dbName)
    .addQueryParameter("u", connectionData.user)
    .addQueryParameter("p", connectionData.password)

  override def sendData(writerData: WriterData[String]): Future[Unit] = {
    val influxData = writerData.data
    val request: Req = influxdbWriteUrl.POST.setBody(influxData.getBytes("UTF-8"))
    logResult {
      httpClient(request > (response => response))
    }.map(_ => ())
  }

  // Keep it lazy. See https://github.com/eed3si9n/scalaxb/pull/279
  private lazy val httpClient = Http().configure(builder =>
    builder.setAllowPoolingConnection(true)
      .setMaximumConnectionsPerHost(1)
      .setMaximumConnectionsTotal(1)
      .setRequestTimeoutInMs(requestTimeout.toMillis.toInt)
  )

  private def logResult: (Future[Response] => Future[Response]) = result => {
    result onComplete {
      case Success(response) if response.getStatusCode == InfluxSuccessStatusCode => logger.info("Data was sent and successfully written")
      case Success(response) => logger.warn(s"Influxdb cannot handle request with metrics: [${response.getResponseBody}]")
      case Failure(ex) => logger.error("Influxdb cannot handle request with metrics:", ex)
    }
    result
  }
}

case class ConnectionData(address: String, port: Int, dbName: String, user: String, password: String)