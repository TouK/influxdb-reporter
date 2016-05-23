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
import influxdbreporter.core.metrics.Metric.CodahaleHistogram
import HistogramCollector._

sealed class HistogramCollector(fieldFM: Field => Option[Field] = t => Some(t))
  extends BaseMetricCollector[CodahaleHistogram, HistogramCollector](fieldFM) {

  override protected def measurementName: String = "histogram"

  override protected def fields(histogram: CodahaleHistogram): List[Field] = {
    val snapshot = histogram.getSnapshot
    Map(
      CountField -> snapshot.size,
      MinField -> snapshot.getMin,
      MaxField -> snapshot.getMax,
      MeanField -> snapshot.getMean,
      MedianField -> snapshot.getMedian,
      StdDevField -> snapshot.getStdDev,
      Percentile50Field -> snapshot.getMedian,
      Percentile75Field -> snapshot.get75thPercentile(),
      Percentile95Field -> snapshot.get95thPercentile(),
      Percentile99Field -> snapshot.get99thPercentile(),
      Percentile999Field -> snapshot.get999thPercentile(),
      RunCountField -> histogram.getCount
    ).map {
      case (key, value) => Field(key, value)
    }.toList
  }

  override def withFieldFlatMap(fieldFM: (Field) => Option[Field]): HistogramCollector = {
    new HistogramCollector(fieldFM)
  }
}

object HistogramCollector {
  val CountField = "count"
  val MinField = "min"
  val MaxField = "max"
  val MeanField = "mean"
  val MedianField = "median"
  val StdDevField = "std-dev"
  val Percentile50Field = "50-percentile"
  val Percentile75Field = "75-percentile"
  val Percentile95Field = "95-percentile"
  val Percentile99Field = "99-percentile"
  val Percentile999Field = "999-percentile"
  val RunCountField = "run-count"

  def apply(): HistogramCollector = new HistogramCollector()
}