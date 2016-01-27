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

import Metric.CodehaleMetric
import MetricByTag.{InfluxdbTags, MetricByTags}
import influxdbreporter.core.Tag

import scala.concurrent.{ExecutionContext, Future}

trait Metric[T <: CodehaleMetric] {

  def popMetrics(implicit ec: ExecutionContext): Future[MetricByTags[T]]
}

object Metric {
  type CodehaleMetric = com.codahale.metrics.Metric
  type CodehaleCounter = com.codahale.metrics.Counter
  type CodehaleHistogram = com.codahale.metrics.Histogram
  type CodehaleMeter = com.codahale.metrics.Meter
  type CodehaleGauge[T] = com.codahale.metrics.Gauge[T]
  type CodehaleTimer = com.codahale.metrics.Timer
}

object MetricByTag {
  type InfluxdbTags = List[Tag]
  type MetricByTags[U <: CodehaleMetric] = List[MetricByTag[U]]
}

case class MetricByTag[U <: CodehaleMetric](tags: InfluxdbTags, metric: U)
