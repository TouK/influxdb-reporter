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
package influxdbreporter.core.metrics.pull

import com.codahale.metrics.Gauge
import influxdbreporter.core.metrics.Metric.CodehaleGauge
import influxdbreporter.core.metrics.MetricByTag.{InfluxdbTags, MetricByTags}
import influxdbreporter.core.metrics.{Metric, MetricByTag}

import scala.concurrent.{ExecutionContext, Future}

abstract class PullingGauge[V] extends Metric[CodehaleGauge[V]] {
  override def popMetrics(implicit ec: ExecutionContext): Future[MetricByTags[CodehaleGauge[V]]] =
    getValues.map {
      _.map {
        case ValueByTag(tags, value) =>
          MetricByTag[CodehaleGauge[V]](tags, new Gauge[V] {
          override def getValue: V = value
        })
      }
    }

  def getValues(implicit ec: ExecutionContext): Future[List[ValueByTag[V]]]
}

case class ValueByTag[V](tags: InfluxdbTags, value: V)