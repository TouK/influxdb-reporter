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
package influxdbreporter.core

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import influxdbreporter.core.metrics.push.Counter
import org.scalatest.WordSpec
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class InfluxdbReporterTests extends WordSpec with ScalaFutures {

  "Batch reportings should be invoking sequentially" in {

    implicit val executionContext = ExecutionContext.global

    val metricsRegistry = MetricRegistry("simple")
    val random = new Random()

    val counter = new AtomicInteger(0)
    val waiter = new Waiter

    val metricsClient = new MetricClient[String] {
      @volatile private var isSending = false

      override def sendData(data: List[WriterData[String]]): Future[Boolean] = {
        synchronized {
          if (isSending) waiter(fail("Concurrent sending"))
          isSending = true
        }
        Thread sleep 50
        val result = if (random.nextBoolean()) Future.successful(random.nextBoolean())
        else Future.failed(new Exception("eg. timeout"))
        result andThen { case _ =>
          synchronized {
            isSending = false
          }
          counter.incrementAndGet()
        }
      }
    }

    (0 to 100) foreach { case idx =>
      val counter = metricsRegistry.register(s"mycounter-$idx", new Counter)
      counter.inc()
    }
    val reporter = createReporter(metricsClient, metricsRegistry)
    reporter.start()

    Thread sleep 6000
  }

  "Reporter cannot be started twice" in {
    val reporter = createReporter(new SkipSendingClient)
    reporter.start()
    intercept[ReporterAlreadyStartedException] {
      reporter.start()
    }
  }

  "Reporter can be started again only when previously started task was stopped" in {
    val reporter = createReporter(new SkipSendingClient)
    val task = reporter.start()
    task.stop()
    reporter.start()
  }

  private def createReporter(metricsClient: MetricClient[String],
                             metricsRegistry: MetricRegistry = MetricRegistry("simple")) = {
    implicit val executionContext = ExecutionContext.global
    new InfluxdbReporter(metricsRegistry,
      LineProtocolWriter,
      metricsClient,
      FiniteDuration(500, TimeUnit.MILLISECONDS),
      new SimpleBatcher(5),
      None
    )
  }
}