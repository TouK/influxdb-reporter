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

import influxdbreporter.core.Field
import influxdbreporter.core.metrics.Metric.CodahaleCounter
import CounterCollector.CountField

sealed class CounterCollector(fieldFM: Field => Option[Field] = t => Some(t))
  extends BaseMetricCollector[CodahaleCounter, CounterCollector](fieldFM) {

  override protected def measurementName: String = "counter"

  override protected def fields(metric: CodahaleCounter): List[Field] = List(Field(CountField, metric.getCount))

  override def withFieldFlatMap(fieldFM: (Field) => Option[Field]): CounterCollector = {
    new CounterCollector(fieldFM)
  }
}

object CounterCollector {
  val CountField = "count"

  def apply(): CounterCollector = new CounterCollector()
}