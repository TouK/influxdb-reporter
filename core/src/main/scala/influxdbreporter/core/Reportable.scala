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

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.typesafe.scalalogging.slf4j.LazyLogging
import influxdbreporter.core.collectors.MetricCollector
import influxdbreporter.core.metrics.Metric
import influxdbreporter.core.metrics.Metric.CodehaleMetric

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

trait Reporter {

  def start(): StoppableReportingTask
}

trait Reportable[S] {

  protected def collectMetrics[M <: CodehaleMetric](metrics: Map[String, (Metric[M], MetricCollector[M])]): Future[List[WriterData[S]]]

  protected def reportMetrics(collectedMetricsData: List[WriterData[S]]): Future[Boolean]
}

abstract class ScheduledReporter[S](metricRegistry: MetricRegistry,
                                    interval: FiniteDuration,
                                    batcher: Batcher[S],
                                    ringBuffer: Option[MetricsRingBuffer[S]])
                                   (implicit executionContext: ExecutionContext)
  extends Reporter with Reportable[S] with LazyLogging {

  private val scheduler = Executors.newScheduledThreadPool(1)
  private var currentStoppableReportingTask: Option[StoppableReportingTaskWithRescheduling] = None

  override def start(): StoppableReportingTask = synchronized {
    if (currentStoppableReportingTask.forall(_.isStopped)) {
      val stoppableTask = new StoppableReportingTaskWithRescheduling(scheduler, createTaskJob, interval)
      currentStoppableReportingTask = Some(stoppableTask)
      stoppableTask
    } else {
      throw new ReporterAlreadyStartedException()
    }
  }

  private def createTaskJob(rescheduler: Reschedulable) = new Runnable {
    override def run(): Unit = {
      reportCollectedMetricsAndRescheduleReporting(rescheduler.reschedule())
    }
  }

  private def reportCollectedMetricsAndRescheduleReporting(reschedule: => Unit) = {
    try {
      reportCollectedMetrics() andThen {
        case Failure(ex) => logger.error("Reporting error:", ex)
      } andThen {
        case _ => reschedule
      }
    } catch {
      case t: Throwable =>
        logger.error("Collecting error:", t)
        reschedule
    }
  }

  private def reportCollectedMetrics() = {
    val collectedMetricsFuture = synchronized {
      collectMetrics(metricRegistry.getMetricsMap)
    }
    for {
      collectedMetrics <- collectedMetricsFuture
      notYetSendMetrics = getNotYetSentMetrics(collectedMetrics)
      batches = batcher.partition(notYetSendMetrics)
      reported <- reportMetricBatchesSequentially(batches) {
        reportMetrics
      }
      successfulSentMetrics = reported filter (_.reported) flatMap (_.batch)
      _ = clearSentMetricsResources(successfulSentMetrics)
    } yield reported
  }

  private def getNotYetSentMetrics(collectedMetrics: List[WriterData[S]]): List[WriterData[S]] = {
    ringBuffer.map(_.add(collectedMetrics)).getOrElse(collectedMetrics)
  }

  private def clearSentMetricsResources(sentMetrics: List[WriterData[S]]): Unit = {
    ringBuffer.map(_.remove(sentMetrics))
  }

  private def reportMetricBatchesSequentially[T](batches: TraversableOnce[List[WriterData[T]]])
                                                (func: List[WriterData[T]] => Future[Boolean]): Future[List[BatchReportingResult[T]]] = {
    batches.foldLeft(Future.successful[List[BatchReportingResult[T]]](Nil)) {
      (acc, batch) => acc.flatMap { accList =>
        func(batch)
          .map(BatchReportingResult(batch, _) :: accList)
          .recover { case ex =>
            logger.error("Batch reporting error:", ex)
            BatchReportingResult(batch, reported = false) :: accList
          }
      }
    }
  }

  private case class BatchReportingResult[T](batch: List[WriterData[T]], reported: Boolean)

}

case class ReporterAlreadyStartedException() extends Exception("Reporter has been already started")

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
  private var stopped = false
  private var currentScheduledTask = scheduleNextTask()

  override def stop(): Unit = synchronized {
    stopped = true
    if (!currentScheduledTask.isCancelled) currentScheduledTask.cancel(false)
  }

  def isStopped: Boolean = synchronized(stopped)

  override def reschedule(): Unit = synchronized {
    if (!stopped) {
      currentScheduledTask = scheduleNextTask()
    }
  }

  private def scheduleNextTask() =
    scheduler.schedule(task, delay.toMillis, TimeUnit.MILLISECONDS)
}
