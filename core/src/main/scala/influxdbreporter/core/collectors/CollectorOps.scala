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
import influxdbreporter.core.metrics.Metric
import Metric._
import influxdbreporter.core.metrics.Metric


trait CollectorOps[T <: CodehaleMetric] {
  def collector: MetricCollector[T]
}

object CollectorOps {

  implicit object CollectorForCounter extends CollectorOps[CodehaleCounter] {
    override def collector: MetricCollector[CodehaleCounter] = CounterCollector
  }

  implicit def CollectorForGauge[T] = new CollectorOps[Gauge[T]] {
    override def collector: MetricCollector[Gauge[T]] = new GaugeCollector[T]
  }

  implicit object CollectorForHistogram extends CollectorOps[CodehaleHistogram] {
    override def collector: MetricCollector[CodehaleHistogram] = HistogramCollector
  }

  implicit object CollectorForMeter extends CollectorOps[CodehaleMeter] {
    override def collector: MetricCollector[CodehaleMeter] = MeterCollector
  }

  implicit object CollectorForTimer extends CollectorOps[CodehaleTimer] {
    override def collector: MetricCollector[CodehaleTimer] = SecondTimerCollector
  }

}