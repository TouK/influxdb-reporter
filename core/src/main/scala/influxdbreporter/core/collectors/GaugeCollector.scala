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
package influxdbreporter.core.collectors

import com.codahale.metrics.Gauge
import influxdbreporter.core.{Field, Tag}
import GaugeCollector.ValueField

sealed class GaugeCollector[T](staticTags: List[Tag] = Nil, fieldFM: Field => Option[Field] = t => Some(t))
  extends BaseMetricCollector[Gauge[T], GaugeCollector[T]](staticTags, fieldFM) {

  override protected def measurementName: String = "gauge"

  override protected def fields(gauge: Gauge[T]): List[Field] = List(Field(ValueField, gauge.getValue))

  override def withFieldMapper(mapper: (Field) => Option[Field]): GaugeCollector[T] =
    new GaugeCollector[T](staticTags, mapper)

  override def withStaticTags(tags: List[Tag]): GaugeCollector[T] =
    new GaugeCollector[T](tags, fieldFM)
}

object GaugeCollector {
  val ValueField = "value"

  def apply[T](): GaugeCollector[T] = new GaugeCollector[T]()
}
