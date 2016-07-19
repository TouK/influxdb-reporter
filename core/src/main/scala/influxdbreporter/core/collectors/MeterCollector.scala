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

import influxdbreporter.core.{Field, Tag}
import influxdbreporter.core.metrics.Metric.CodahaleMeter
import MeterCollector._

sealed class MeterCollector(staticTags: List[Tag] = Nil, fieldFM: Field => Option[Field] = t => Some(t))
  extends BaseMetricCollector[CodahaleMeter, MeterCollector](staticTags, fieldFM) {

  override protected def measurementName: String = "meter"

  override protected def fields(meter: CodahaleMeter): List[Field] = {
    Map(
      CountField -> meter.getCount,
      OneMinuteField -> meter.getOneMinuteRate,
      FiveMinuteField -> meter.getFiveMinuteRate,
      FifteenMinuteField -> meter.getFifteenMinuteRate,
      MeanRateField -> meter.getMeanRate
    ).map {
      case (key, value) => Field(key, value)
    }.toList
  }

  override def withFieldMapper(mapper: (Field) => Option[Field]): MeterCollector =
    new MeterCollector(staticTags, mapper)

  override def withStaticTags(tags: List[Tag]): MeterCollector =
    new MeterCollector(tags, fieldFM)
}

object MeterCollector {
  val CountField = "count"
  val OneMinuteField = "one-minute"
  val FiveMinuteField = "five-minute"
  val FifteenMinuteField = "fifteen-minute"
  val MeanRateField = "mean-rate"

  def apply(): MeterCollector = new MeterCollector()
}
