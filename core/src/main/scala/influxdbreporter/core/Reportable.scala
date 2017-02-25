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

import com.codahale.metrics.Clock
import com.typesafe.scalalogging.LazyLogging
import influxdbreporter.core.writers.Writer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Failure

trait Reporter {

  def start(): StoppableReportingTask
}

abstract class ScheduledReporter[S](metricRegistry: MetricRegistry,
                                    interval: FiniteDuration,
                                    writer: Writer[S],
                                    clientFactory: MetricClientFactory[S],
                                    batcher: Batcher[S],
                                    buffer: Option[WriterDataBuffer[S]],
                                    clock: Clock)
                                   (implicit executionContext: ExecutionContext)
  extends BaseReporter[S](metricRegistry, writer, batcher, buffer, clock) {

  private val scheduler = Executors.newScheduledThreadPool(1)
  private var currentStoppableReportingTask: Option[StoppableReportingTaskWithRescheduling] = None

  override def start(): StoppableReportingTask = synchronized {
    if (currentStoppableReportingTask.forall(_.isStopped)) {
      val client = clientFactory.create()
      val stoppableTask = new StoppableReportingTaskWithRescheduling(scheduler, createTaskJob(client), interval, client)
      currentStoppableReportingTask = Some(stoppableTask)
      logger.info(s"Influxdb scheduled reporter was started with $interval report interval")
      stoppableTask
    } else {
      throw ReporterAlreadyStartedException
    }
  }

  private def createTaskJob(client: MetricClient[S])(rescheduler: Reschedulable) = new Runnable {
    override def run(): Unit = {
      reportCollectedMetricsAndRescheduleReporting(client, rescheduler.reschedule())
    }
  }

  private def reportCollectedMetricsAndRescheduleReporting(client: MetricClient[S], reschedule: => Unit) = {
    try {
      reportCollectedMetrics(client) andThen {
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
}

case object ReporterAlreadyStartedException extends Exception("Reporter has been already started")

trait StoppableReportingTask {
  def stop(): Unit
  def isStopped: Boolean
}

trait Reschedulable {
  def reschedule(): Unit
}

private class StoppableReportingTaskWithRescheduling(scheduler: ScheduledExecutorService,
                                                     createTask: Reschedulable => Runnable,
                                                     delay: FiniteDuration,
                                                     client: MetricClient[_])
  extends StoppableReportingTask with Reschedulable with LazyLogging {

  private val task = createTask(this)
  private var stopped = false
  private var currentScheduledTask = scheduleNextTask()

  override def stop(): Unit = synchronized {
    stopped = true
    if (!currentScheduledTask.isCancelled) currentScheduledTask.cancel(false)
    client.stop()
    logger.info(s"Influxdb scheduled reporter was stopped!")
  }

  override def isStopped: Boolean = synchronized(stopped)

  override def reschedule(): Unit = synchronized {
    if (!stopped) {
      currentScheduledTask = scheduleNextTask()
    }
  }

  private def scheduleNextTask() =
    scheduler.schedule(task, delay.toMillis, TimeUnit.MILLISECONDS)
}
