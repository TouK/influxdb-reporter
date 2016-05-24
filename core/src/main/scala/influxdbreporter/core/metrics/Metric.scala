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
package influxdbreporter.core.metrics

import Metric.CodahaleMetric
import MetricByTag.{InfluxdbTags, MetricByTags}
import influxdbreporter.core.Tag

import scala.concurrent.{ExecutionContext, Future}

trait Metric[T <: CodahaleMetric] {

  def popMetrics(implicit ec: ExecutionContext): Future[MetricByTags[T]]

  def map[NT <: CodahaleMetric](f: T => NT): Metric[NT] = new Metric[NT] {
    override def popMetrics(implicit ec: ExecutionContext): Future[MetricByTags[NT]] =
      Metric.this.popMetrics.map { metrics =>
        metrics.map { metric =>
          metric.map(f)
        }
      }
  }

  def mapAll[NT <: CodahaleMetric](f: MetricByTags[T] => MetricByTags[NT]): Metric[NT] = new Metric[NT] {
    override def popMetrics(implicit ec: ExecutionContext): Future[MetricByTags[NT]] =
      Metric.this.popMetrics.map(f)
  }

}

object Metric {
  type CodahaleMetric = com.codahale.metrics.Metric
  type CodahaleCounter = com.codahale.metrics.Counter
  type CodahaleHistogram = com.codahale.metrics.Histogram
  type CodahaleMeter = com.codahale.metrics.Meter
  type CodahaleGauge[T] = com.codahale.metrics.Gauge[T]
  type CodahaleTimer = com.codahale.metrics.Timer
}

object MetricByTag {
  type InfluxdbTags = List[Tag]
  type MetricByTags[U <: CodahaleMetric] = List[MetricByTag[U]]
}

case class MetricByTag[U <: CodahaleMetric](tags: InfluxdbTags, metric: U, timestamp: Option[Long] = None) {
  def withTag(tag: Tag): MetricByTag[U] = {
    copy(tags = tag :: tags)
  }

  def map[NU <: CodahaleMetric](f: U => NU): MetricByTag[NU] = {
    copy(metric = f(metric))
  }
}
