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

import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.scalalogging.slf4j.LazyLogging
import influxdbreporter.core.collectors.MetricCollector
import influxdbreporter.core.metrics.Metric
import influxdbreporter.core.metrics.Metric.CodehaleMetric

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

trait Reporter {

  def start(): StoppableReportingTask
}

trait Reportable[S] {

  protected def collectMetrics[M <: CodehaleMetric](metrics: Map[String, (Metric[M], MetricCollector[M])]): Option[WriterData[S]]

  protected def collectCodehaleMetrics[M <: CodehaleMetric](metrics: Map[String, (M, MetricCollector[M])]): Option[WriterData[S]]

  protected def reportMetrics(collectedMetricsData: Option[WriterData[S]], collectedCodehaleMetricsData: Option[WriterData[S]]): Future[Boolean]
}

trait StoppableReportingTask {
  def stop(): Unit
}

abstract class ScheduledReporter[S](metricRegistry: MetricRegistry, interval: FiniteDuration)
                                   (implicit executionContext: ExecutionContext)
  extends Reporter with Reportable[S] with LazyLogging {
  private val scheduler = Executors.newScheduledThreadPool(1)
  private val taskJob = new Runnable {
    override def run(): Unit = {
      try {
        val (collectedMetrics, collectedCodehaleMetrics) = synchronized {
          (collectMetrics(metricRegistry.getMetricsMap), collectCodehaleMetrics(metricRegistry.getCodehaleMetricsMap))
        }
        reportMetrics(collectedMetrics, collectedCodehaleMetrics) onComplete {
          case Success(_) =>
          case Failure(ex) => logger.error("Reporting error:", ex)
        }
      } catch {
        case t: Throwable => logger.error("Collecting error:", t)
      }
    }
  }

  override def start(): StoppableReportingTask = new StoppableReportingTask {
    val reportingTask = scheduler.scheduleWithFixedDelay(taskJob, interval.toMillis, interval.toMillis, TimeUnit.MILLISECONDS)

    override def stop(): Unit = {
      if (!reportingTask.isCancelled) reportingTask.cancel(true)
    }
  }

}