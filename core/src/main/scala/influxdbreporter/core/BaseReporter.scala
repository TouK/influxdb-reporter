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

import com.codahale.metrics.Clock
import com.typesafe.scalalogging.slf4j.LazyLogging
import influxdbreporter.core.collectors.MetricCollector
import influxdbreporter.core.metrics.{Metric, MetricByTag}
import influxdbreporter.core.metrics.Metric._

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseReporter[S](metricRegistry: MetricRegistry,
                               writer: Writer[S],
                               batcher: Batcher[S],
                               cache: Option[WriterDataCache[S]],
                               clock: Clock)
                              (implicit ec: ExecutionContext)
  extends Reporter with Reportable[S] with LazyLogging {

  protected def reportCollectedMetrics(): Future[List[BatchReportingResult[S]]] = {
    val collectedMetricsFuture = synchronized {
      collectMetrics(metricRegistry.getMetricsMap)
    }
    for {
      collectedMetrics <- collectedMetricsFuture
      notYetSendMetrics = collectedMetrics ::: notYetSentMetricsFromCache
      batches = batcher.partition(notYetSendMetrics)
      reported <- reportMetricBatchesSequentially(batches) {
        reportMetrics
      }
      _ = updateNotSentMetricsCache(reported)
    } yield reported
  }

  private def notYetSentMetricsFromCache: List[WriterData[S]] = {
    cache map (_.get()) getOrElse Nil
  }

  private def updateNotSentMetricsCache(reportResult: List[BatchReportingResult[S]]): Unit = {
    val (sent, notSent) = reportResult partition (_.reported)
    val notSentMetrics = notSent flatMap (_.batch)
    val sentMetrics = sent flatMap (_.batch)
    cache.map(_.update(notSentMetrics, sentMetrics))
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

  private def collectMetrics[M <: CodehaleMetric](metrics: Map[String, (Metric[M], MetricCollector[M])]): Future[List[WriterData[S]]] = {
    val timestamp = clock.getTick
    Future.sequence(metrics.toList.map {
      case (name, (metric, collector)) =>
        metric.popMetrics.map {
          _.map {
            case MetricByTag(tags, m, timestampOpt) =>
              collector.collect(writer, name, m, timestampOpt.getOrElse(timestamp), tags: _*)
          }
        }
    }).map(listOfLists => listOfLists.flatten)
  }

  protected case class BatchReportingResult[T](batch: List[WriterData[T]], reported: Boolean)

}