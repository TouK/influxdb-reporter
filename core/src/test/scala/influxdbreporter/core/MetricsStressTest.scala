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

import influxdbreporter.core.collectors.{CounterCollector, MeterCollector, MetricCollector, SecondTimerCollector}
import influxdbreporter.core.metrics.Metric.{CodehaleCounter, CodehaleMeter, CodehaleMetric, CodehaleTimer}
import influxdbreporter.core.metrics.push.{Counter, Timer, Meter}
import influxdbreporter.core.metrics.Metric
import org.scalatest.WordSpec
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class MetricsStressTest extends WordSpec with ScalaFutures {

  private val SINGLE_REPORT_REPETITION = 1000
  private val CONCURRENT_REPORTS_COUNT = 10
  private val TAGS_MAX_COUNT = 10

  private val influxTags = (1 to TAGS_MAX_COUNT).map(i => Tag(s"tag$i", i)).toList

  "A stress test for InfluxDbReporter" in {

    implicit val executionContext = ExecutionContext.global

    val metricsRegistry = MetricRegistry("stress")

    val counter = new Counter
    val counterCollector = new CountableCollector[CodehaleCounter](CounterCollector)
    metricsRegistry.register("mycounter", (counter, counterCollector))

    val timer = new Timer
    val timerCollector = new CountableCollector[CodehaleTimer](SecondTimerCollector)
    metricsRegistry.register("mytimer", (timer, timerCollector))

    val meter = new Meter
    val meterCollector = new CountableCollector[CodehaleMeter](MeterCollector)
    metricsRegistry.register("mymeter", (meter, meterCollector))

    val metricContextList = List(
      new MetricTestContext[CodehaleCounter, Counter](counter, counterCollector) {
        override protected def updateMetric(metric: Counter, tags: List[Tag]): Unit = {
          metric.inc(tags: _*)
        }

        override protected def usageCount(metric: CodehaleCounter, lastCount: Int): Int = lastCount + metric.getCount.asInstanceOf[Int]
      },
      new MetricTestContext[CodehaleTimer, Timer](timer, timerCollector) {
        override protected def updateMetric(metric: Timer, tags: List[Tag]): Unit = {
          val context = metric.time(tags: _*)
          Thread.sleep(10)
          context.stop()
        }

        override protected def usageCount(metric: CodehaleTimer, lastCount: Int): Int = lastCount + metric.getCount.asInstanceOf[Int]
      },
      new MetricTestContext[CodehaleMeter, Meter](meter, meterCollector) {
        override protected def updateMetric(metric: Meter, tags: List[Tag]): Unit = metric.mark(tags: _*)

        override protected def usageCount(metric: CodehaleMeter, lastCount: Int): Int = lastCount + metric.getCount.asInstanceOf[Int]
      }
    )

    val w = new Waiter

    val metricsClient = new MetricClient[String] {
      override def sendData(data: WriterData[String]): Future[Unit] = {
        if (!areCorrect(data.data)) {
          w.dismiss()
        }
        Future.successful(())
      }
    }

    val reporter = new InfluxdbReporter(metricsRegistry, LineProtocolWriter, metricsClient, FiniteDuration(500, TimeUnit.MILLISECONDS))
    reporter.start()

    val simulations = (0 until CONCURRENT_REPORTS_COUNT).map { _ =>
      Future(simulateReporting(metricContextList))
    }

    for {
      _ <- Future.sequence(simulations)
      _ = Thread.sleep(1000)
    } yield w.dismiss()

    val wait = 360
    val testTimeout = org.scalatest.concurrent.PatienceConfiguration.Timeout(Span(wait, Seconds))
    w.await(testTimeout)

    metricContextList.foreach { context =>
      assert(context.isCorrect, "Stress test not passed!")
    }
  }

  private def simulateReporting(contextList: List[MetricTestContext[_, _]])
                               (implicit executionContext: ExecutionContext): Unit = {
    val random = new Random()
    (0 until SINGLE_REPORT_REPETITION).foreach { _ =>
      val randomMetricIndex = random.nextInt(contextList.length)
      contextList(randomMetricIndex).update()
      Thread.sleep(15)
    }
  }

  private def areCorrect(data: String): Boolean = data.split("\n").forall(line =>
    line.startsWith("stress.") && (line.filter(_ == ' ').length == 2)
  )

  private abstract class MetricTestContext[S <: CodehaleMetric, T <: Metric[S]](metric: T, collector: CountableCollector[S])
    extends CollectListener[S] {
    private val usedCount = new AtomicInteger(0)
    private val collectedCount: AtomicInteger = new AtomicInteger(0)

    collector.setCollectListener(this)

    protected def updateMetric(metric: T, tags: List[Tag]): Unit

    protected def usageCount(metric: S, lastCount: Int): Int

    def update(): Unit = {
      updateMetric(metric, randomTags)
      usedCount.incrementAndGet()
    }

    private def randomTags: List[Tag] = {
      val random = new Random()
      def next = random.nextInt(influxTags.length)
      val result = 0.until(next).foldLeft(Set.empty[Tag]) {
        case (acc, _) => acc + influxTags(next)
      }.toList
      result
    }

    def isCorrect: Boolean = usedCount.get() == collectedCount.get()

    override def onCollect(metric: S) = collectedCount.set(usageCount(metric, collectedCount.get()))
  }

  private trait CollectListener[T <: CodehaleMetric] {
    def onCollect(metric: T): Unit
  }

  private class CountableCollector[S <: CodehaleMetric](collector: MetricCollector[S]) extends MetricCollector[S] {

    private var listener: Option[CollectListener[S]] = None

    def setCollectListener(collectorListener: CollectListener[S]): Unit = {
      listener = Some(collectorListener)
    }

    override def collect[U](writer: Writer[U], name: String, metric: S, timestamp: Long, tags: Tag*): WriterData[U] = {
      listener.foreach(_.onCollect(metric))
      collector.collect(writer, name, metric, timestamp, tags: _*)
    }
  }

}