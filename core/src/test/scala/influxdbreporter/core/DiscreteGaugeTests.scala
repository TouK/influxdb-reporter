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

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import influxdbreporter.core.metrics.push.DiscreteGauge
import influxdbreporter.core.writers.WriterData
import org.scalatest.time.SpanSugar._

import scala.concurrent.Future
import scala.language.postfixOps

class DiscreteGaugeTests extends BaseMetricTest with TestReporterProvider {

  "A DiscreteGauge" should {

    "be thread safe" in {

      val working = new AtomicBoolean(true)
      val addedValuesCount = new AtomicInteger(0)
      val reportedValuesCount = new AtomicInteger(0)

      val registry = MetricRegistry("test")
      val tags = Tag("a", 1) :: Tag("b", 2) :: Tag("c", 3) :: Nil
      val gauge = registry.register("dg", new DiscreteGauge[Int])
      val clientFactory = new MetricClientFactory[String] {
        override def create(): MetricClient[String] = new MetricClient[String] {
          override def sendData(data: List[WriterData[String]]): Future[Boolean] = {
            reportedValuesCount.addAndGet(data.length)
            Future.successful(true)
          }
          override def stop(): Unit = {}
        }
      }

      val reporter = createReporter(clientFactory, registry)
      reporter.start()

      val reportingTasks = (0 until 4).map { _ =>
        Future {
          while (working.get()) {
            gauge.addValue(1, tags: _*)
            addedValuesCount.incrementAndGet()
            Thread.sleep(1)
          }
        }
      }

      Thread.sleep(6000)
      working.set(false)
      Thread.sleep(6000)

      assertResult(reportedValuesCount.get())(addedValuesCount.get())
    }

    "add uniqueness ensuring tag for each gauge metric" in {
      val tags = Tag("a", 1) :: Tag("b", 2) :: Tag("c", 3) :: Nil
      val gauge = new DiscreteGauge[Int]
      gauge.addValue(100, tags: _*)
      gauge.addValue(200, tags: _*)
      gauge.addValue(50, tags.tail: _*)

      whenReady(gauge.popMetrics, timeout(5 seconds)) { res =>
        res.foreach(metricByTags =>
          assert(metricByTags.tags.exists(_.key == "u"))
        )
      }
    }
  }

}