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

import influxdbreporter.core.metrics.Metric.CodehaleHistogram
import influxdbreporter.core.{Field, Tag, Writer, WriterData}

object HistogramCollector extends MetricCollector[CodehaleHistogram] {

  override def collect[U](writer: Writer[U],
                          name: String,
                          metric: CodehaleHistogram,
                          timestamp: Long,
                          tags: Tag*): WriterData[U] =
    writer.write(s"$name.histogram", fields(metric), tags.toList, timestamp)

  private def fields(histogram: CodehaleHistogram): List[Field] = {
    val snapshot = histogram.getSnapshot
    Map(
      "count" -> snapshot.size,
      "min" -> snapshot.getMin,
      "max" -> snapshot.getMax,
      "mean" -> snapshot.getMean,
      "median" -> snapshot.getMedian,
      "std-dev" -> snapshot.getStdDev,
      "50-percentile" -> snapshot.getMedian,
      "75-percentile" -> snapshot.get75thPercentile(),
      "95-percentile" -> snapshot.get95thPercentile(),
      "99-percentile" -> snapshot.get99thPercentile(),
      "999-percentile" -> snapshot.get999thPercentile(),
      "run-count" -> histogram.getCount
    ).map {
      case (key, value) => Field(key, value)
    }.toList
  }
}
