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

import influxdbreporter.core.collectors.MetricCollector
import influxdbreporter.core.metrics.Metric
import influxdbreporter.core.metrics.Metric.CodehaleMetric

import scala.concurrent.duration.FiniteDuration

trait Reporter {

  def start(): StoppableReportingTask
}

trait Reportable {

  protected def reportMetrics[M <: CodehaleMetric](metrics: Map[String, (Metric[M], MetricCollector[M])]): Unit

  protected def reportCodehaleMetrics[M <: CodehaleMetric](metrics: Map[String, (M, MetricCollector[M])]): Unit
}

trait StoppableReportingTask {
  def stop(): Unit
}

abstract class ScheduledReporter(metricRegistry: MetricRegistry,
                                 interval: FiniteDuration) extends Reporter with Reportable {
  private val scheduler = Executors.newScheduledThreadPool(1)
  private val taskJob = new Runnable {
    override def run(): Unit = synchronized {
      reportMetrics(metricRegistry.getMetricsMap)
      reportCodehaleMetrics(metricRegistry.getCodehaleMetricsMap)
    }
  }

  override def start(): StoppableReportingTask = new StoppableReportingTask {
    val reportingTask = scheduler.scheduleWithFixedDelay(taskJob, interval.toMillis, interval.toMillis, TimeUnit.MILLISECONDS)

    override def stop(): Unit = {
      if (!reportingTask.isCancelled) reportingTask.cancel(true)
    }
  }

}