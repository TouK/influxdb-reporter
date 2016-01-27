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

import java.util.concurrent.{ScheduledExecutorService, Executors, TimeUnit}

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

  protected def reportMetrics(collectedMetricsData: Option[WriterData[S]]): Future[Boolean]
}

abstract class ScheduledReporter[S](metricRegistry: MetricRegistry, interval: FiniteDuration)
                                   (implicit executionContext: ExecutionContext)
  extends Reporter with Reportable[S] with LazyLogging {

  private val scheduler = Executors.newScheduledThreadPool(1)

  override def start(): StoppableReportingTask =
    new StoppableReportingTaskWithRescheduling(scheduler, createTaskJob, interval)

  private def createTaskJob(rescheduler: Reschedulable) = new Runnable {
    override def run(): Unit = {
      reportCollectedMetricsAndRescheduleReporting(rescheduler.reschedule())
    }
  }

  private def reportCollectedMetricsAndRescheduleReporting(reschedule: => Unit) = {
    try {
      val collectedMetrics = synchronized {
        collectMetrics(metricRegistry.getMetricsMap)
      }
      reportMetrics(collectedMetrics) onComplete {
        case Success(_) =>
          reschedule
        case Failure(ex) =>
          reschedule
          logger.error("Reporting error:", ex)
      }
    } catch {
      case t: Throwable =>
        reschedule
        logger.error("Collecting error:", t)
    }
  }
}

trait StoppableReportingTask {
  def stop(): Unit
}

trait Reschedulable {
  def reschedule(): Unit
}

private class StoppableReportingTaskWithRescheduling(scheduler: ScheduledExecutorService,
                                                     createTask: Reschedulable => Runnable,
                                                     delay: FiniteDuration)
  extends StoppableReportingTask with Reschedulable {

  private val task = createTask(this)
  @volatile private var isStopped = false
  @volatile private var currentScheduledTask = scheduleNextTask()

  override def stop(): Unit = {
    isStopped = true
    if(!currentScheduledTask.isCancelled) currentScheduledTask.cancel(false)
  }

  override def reschedule(): Unit = if (!isStopped) {
    currentScheduledTask = scheduleNextTask()
  }

  private def scheduleNextTask() =
    scheduler.schedule(task, delay.toMillis, TimeUnit.MILLISECONDS)
}
