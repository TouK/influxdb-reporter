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

import influxdbreporter.core.metrics.Metric._
import influxdbreporter.core.writers.{Writer, WriterData}
import influxdbreporter.core.{Field, Tag}

trait MetricCollector[T <: CodahaleMetric] {

  def collect[U](writer: Writer[U], name: String, metric: T, timestamp: Long, tags: Tag*): Option[WriterData[U]]
}

abstract class BaseMetricCollector[T <: CodahaleMetric, V <: BaseMetricCollector[T, V]](staticTags: List[Tag],
                                                                                        mapper: Field => Option[Field])
  extends MetricCollector[T] {

  protected def measurementName: String

  protected def fields(metric: T): List[Field]

  def withFieldMapper(mapper: Field => Option[Field]): V

  def withStaticTags(tags: List[Tag]): V

  def collect[U](writer: Writer[U], name: String, metric: T, timestamp: Long, tags: Tag*): Option[WriterData[U]] = {
    filterFields(metric) match {
      case Nil =>
        None
      case fields =>
        Some(writer.write(s"$name.$measurementName", filterFields(metric), tags.toSet ++ staticTags, timestamp))
    }
  }

  private def filterFields(metric: T): List[Field] = {
    fields(metric).flatMap(f => mapper(f))
  }

}