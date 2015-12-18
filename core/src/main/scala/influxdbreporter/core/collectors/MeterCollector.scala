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

import influxdbreporter.core.metrics.Metric.CodehaleMeter
import influxdbreporter.core.{Field, Tag, Writer, WriterData}

object MeterCollector extends MetricCollector[CodehaleMeter] {

  override def collect[U](writer: Writer[U],
                          name: String,
                          metric: CodehaleMeter,
                          timestamp: Long,
                          tags: Tag*): WriterData[U] =
    writer.write(s"$name.meter", fields(metric), tags.toList, timestamp)

  private def fields(meter: CodehaleMeter): List[Field] = {
    Map(
      "count" -> meter.getCount,
      "one-minute" -> meter.getOneMinuteRate,
      "five-minute" -> meter.getFiveMinuteRate,
      "fifteen-minute" -> meter.getFifteenMinuteRate,
      "mean-rate" -> meter.getMeanRate
    ).map {
      case (key, value) => Field(key, value)
    }.toList
  }

}
