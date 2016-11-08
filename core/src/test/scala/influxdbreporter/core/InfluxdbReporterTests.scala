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

import java.util.concurrent.atomic.AtomicInteger

import influxdbreporter.core.metrics.push.Counter
import influxdbreporter.core.writers.WriterData
import org.scalatest.WordSpec
import org.scalatest.concurrent.Waiters.Waiter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class InfluxdbReporterTests extends WordSpec with TestReporterProvider with ScalaFutures {

  implicit val executionContext = ExecutionContext.global

  "Batch reportings should be invoking sequentially" in {

    val metricsRegistry = MetricRegistry("simple")
    val random = new Random()

    val counter = new AtomicInteger(0)
    val waiter = new Waiter

    val metricsClient = new SendInvocationCountingMetricsClientDecorator(new SkipSendingClient) {
      @volatile private var isSending = false

      override def sendData(data: List[WriterData[String]]): Future[Boolean] = {
        val result = randomSendingResultWithConcurrencyChecks
        super.sendData(data)
        if(sendInvocationCount == 20) waiter.dismiss()
        result
      }

      private def randomSendingResultWithConcurrencyChecks = {
        synchronized {
          if (isSending) waiter(fail("Concurrent sending"))
          isSending = true
        }
        Thread sleep 50
        val result = if (random.nextBoolean()) Future.successful(random.nextBoolean())
        else Future.failed(new Exception("eg. timeout"))
        result andThen { case _ =>
          synchronized {
            if (!isSending) waiter(fail("Concurrent sending"))
            isSending = false
          }
          counter.incrementAndGet()
        }
      }
    }

    (0 to 100) foreach { idx =>
      val counter = metricsRegistry.register(s"mycounter-$idx", new Counter)
      counter.inc()
    }
    val reporter = createReporter(metricsClient, metricsRegistry)
    reporter.start()

    waiter.await(timeout(6000 millis))
  }

  "Reporter cannot be started twice" in {
    val reporter = createReporter(new SkipSendingClient)
    reporter.start()
    intercept[ReporterAlreadyStartedException.type] {
      reporter.start()
    }
  }

  "Reporter can be started again only when previously started task was stopped" in {
    val reporter = createReporter(new SkipSendingClient)
    val task = reporter.start()
    task.stop()
    reporter.start()
  }

  "Not sent metrics measurements should be buffered when buffer is configured" in {
    val metricsRegistry = MetricRegistry("simple")
    val waiter = new Waiter
    val metricsClient = new SendInvocationCountingMetricsClientDecorator(new SkipSendingClient) {
      override def sendData(data: List[WriterData[String]]): Future[Boolean] = {
        val result: Future[Boolean] = sendInvocationCount match {
          case 0 =>
            if (data.length != 3) waiter(fail(s"Wrong count of measurements (inv no: $sendInvocationCount)"))
            Future.successful(false)
          case 1 =>
            if (data.length != 4) waiter(fail(s"Wrong count of measurements (inv no: $sendInvocationCount)"))
            Future.successful(true)
          case 2 =>
            if (data.length != 1) waiter(fail(s"Wrong count of measurements (inv no: $sendInvocationCount)"))
            Future.successful(true)
          case _ =>
            waiter(fail("Should not happened"))
            Future.failed(new Exception())
        }
        super.sendData(data)
        result
      }
    }

    val counter1 = metricsRegistry.register("c1", new Counter)
    val counter2 = metricsRegistry.register("c2", new Counter)
    val counter3 = metricsRegistry.register("c3", new Counter)

    val reporter = createReporter(metricsClient, metricsRegistry, Some(new FixedSizeWriterDataBuffer(2)))
    reporter.start()

    Future {
      counter1.inc(4)
      counter2.inc()
      counter3.inc(2)

      Thread.sleep(600)

      counter1.inc(2)
      counter2.inc(3)

      Thread.sleep(600)

      counter1.inc(1)

      Thread.sleep(600)
      waiter.dismiss()
    }

    waiter.await(timeout(1900 millis))
  }


  private class SendInvocationCountingMetricsClientDecorator[T](metricClient: MetricClient[T])
                                                               (implicit executionContext: ExecutionContext)
    extends MetricClient[T] {
    @volatile private var sendInvocationCounter = 0

    override def sendData(data: List[WriterData[T]]): Future[Boolean] = {
      val result = metricClient.sendData(data)
      result andThen { case _ =>
        sendInvocationCounter = sendInvocationCounter + 1
      }
    }

    def sendInvocationCount: Int = sendInvocationCounter
  }

}