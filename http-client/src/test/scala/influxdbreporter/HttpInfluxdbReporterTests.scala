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

import java.util.concurrent.TimeUnit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.scalalogging.LazyLogging
import influxdbreporter.core.metrics.push.Counter
import influxdbreporter.core.writers.{LineProtocolWriter, WriterData}
import influxdbreporter.core.{MetricClientFactory, _}
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.Waiters.Waiter

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class HttpInfluxdbReporterTests extends WordSpec with ScalaFutures with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val address = "localhost"
  private val port = 8086
  private val dbName = "exampledb"
  private val user = "root"
  private val password = "pass"
  private val interval = FiniteDuration(500, TimeUnit.MILLISECONDS)

  "Reporter could be started several times" in {
    val wireMockServer = new WireMockServer(port)
    wireMockServer.start()
    val wireMock = new WireMock(address, wireMockServer.port())

    val metricsRegistry = MetricRegistry("simple")

    val counter1 = metricsRegistry.register("c1", new Counter)
    val counter2 = metricsRegistry.register("c2", new Counter)
    val counter3 = metricsRegistry.register("c3", new Counter)

    val waiter = new Waiter
    val reporter = reporterWithWaiterDismissOnSent(metricsRegistry, waiter)

    assertReporterSendingRequests(wireMock, waiter, reporter.start(), counter1, counter2, counter3)
    assertReporterNotSendingRequests(wireMock)
    assertReporterSendingRequests(wireMock, waiter, reporter.start(), counter1, counter2, counter3)

    wireMockServer.stop()
  }

  private def assertReporterSendingRequests(wireMock: WireMock,
                                            waiter: Waiter,
                                            task: StoppableReportingTask,
                                            counter1: Counter,
                                            counter2: Counter,
                                            counter3: Counter) = {
    wireMock.register(influxDbMapping)

    counter1.inc(4)
    counter2.inc()
    counter3.inc(2)

    waiter.await(timeout(interval * 10))
    task.stop()

    wireMock.verifyThat(moreThan(0), influxDbReporterRequestPattern)
  }

  private def assertReporterNotSendingRequests(wireMock: WireMock) = {
    wireMock.resetRequests()
    Thread.sleep(interval * 2 toMillis)
    wireMock.verifyThat(0, influxDbReporterRequestPattern)
  }

  private def influxDbMapping = {
    post(urlPathEqualTo(s"/write"))
      .withQueryParam("db", equalTo(dbName))
      .withQueryParam("u", equalTo(user))
      .withQueryParam("p", equalTo(password))
      .willReturn(aResponse().withStatus(204))
  }

  private def influxDbReporterRequestPattern = {
    postRequestedFor(urlPathEqualTo("/write"))
          .withQueryParam("db", equalTo(dbName))
          .withQueryParam("u", equalTo(user))
          .withQueryParam("p", equalTo(password))
  }

  private def reporterWithWaiterDismissOnSent(metricsRegistry: MetricRegistry, waiter: Waiter) = {
    implicit val timeout = (interval * 9) / 10
    new InfluxdbReporter[String](
      metricsRegistry,
      new LineProtocolWriter(),
      new MetricClientFactory[String] {
        override def create(): MetricClient[String] = new MetricClient[String] {
          val innerClient = new HttpInfluxdbClient(ConnectionData(address, port, dbName, user, password))
          override def stop(): Unit = innerClient.stop()
          override def sendData(data: List[WriterData[String]]): Future[Boolean] = {
            innerClient.sendData(data) andThen { case _ => waiter.dismiss() }
          }
        }
      },
      interval,
      new InfluxBatcher
    )
  }
}