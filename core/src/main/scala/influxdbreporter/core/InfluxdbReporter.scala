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
import influxdbreporter.core.collectors.MetricCollector
import influxdbreporter.core.metrics.Metric.CodehaleMetric
import influxdbreporter.core.metrics.{Metric, MetricByTag}
import influxdbreporter.core.utils.UtcClock

import scala.concurrent.duration.FiniteDuration

class InfluxdbReporter[S](registry: MetricRegistry,
                          writer: Writer[S],
                          client: MetricClient[S],
                          interval: FiniteDuration,
                          clock: Clock = UtcClock)
  extends ScheduledReporter(registry, interval) {

  def withInterval(newInterval: FiniteDuration): InfluxdbReporter[S] =
    new InfluxdbReporter[S](registry, writer, client, newInterval, clock)

  override def reportMetrics[M <: CodehaleMetric](metrics: Map[String, (Metric[M], MetricCollector[M])]): Unit = {
    val timestamp = clock.getTick
    reduceWriterData {
      metrics.toList.flatMap {
        case (name, (metric, collector)) => metric.popMetrics.map {
          case MetricByTag(tags, m) =>
            Some(collector.collect(writer, name, m, timestamp, tags: _*))
        }
      }
    } map {
      case data => client.sendData(data)
    }
  }

  override protected def reportCodehaleMetrics[M <: CodehaleMetric](metrics: Map[String, (M, MetricCollector[M])]): Unit = {
    val timestamp = clock.getTick
    reduceWriterData {
      metrics.toList.map {
        case (name, (metric, collector)) =>
          Some(collector.collect(writer, name, metric, timestamp))
      }
    } map {
      case data => client.sendData(data)
    }
  }

  private def reduceWriterData(writerData: List[Option[WriterData[S]]]): Option[WriterData[S]] = {
    writerData match {
      case Nil => None
      case x :: Nil => x
      case list =>
        list.reduce[Option[WriterData[S]]] {
          case (Some(wr1: WriterData[S]), Some(wr2: WriterData[S])) => Some(wr1 + wr2)
          case (None, Some(wr: WriterData[S])) => Some(wr)
          case (Some(wr: WriterData[S]), None) => Some(wr)
          case (None, None) => None
        }
    }
  }

}
