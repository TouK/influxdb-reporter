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
import influxdbreporter.core.collectors.TimerCollector._
import influxdbreporter.core.metrics.Metric.CodahaleTimer

import scala.concurrent.duration.{TimeUnit, _}

sealed class TimerCollector(timeUnit: TimeUnit,
                            staticTags: List[Tag] = Nil,
                            fieldFM: Field => Option[Field] = t => Some(t))
  extends BaseMetricCollector[CodahaleTimer, TimerCollector](staticTags, fieldFM) {

  override protected def measurementName: String = "timer"

  override protected def fields(timer: CodahaleTimer): List[Field] = {
    val snapshot = timer.getSnapshot
    Map(
      CountField -> snapshot.size,
      MinField -> convertToOtherTimeUnit(snapshot.getMin, NANOSECONDS, timeUnit),
      MaxField -> convertToOtherTimeUnit(snapshot.getMax, NANOSECONDS, timeUnit),
      MeanField -> convertToOtherTimeUnit(snapshot.getMean, NANOSECONDS, timeUnit),
      StdDevField -> convertToOtherTimeUnit(snapshot.getStdDev, NANOSECONDS, timeUnit),
      Percentile50Field -> convertToOtherTimeUnit(snapshot.getMedian, NANOSECONDS, timeUnit),
      Percentile75Field -> convertToOtherTimeUnit(snapshot.get75thPercentile(), NANOSECONDS, timeUnit),
      Percentile95Field -> convertToOtherTimeUnit(snapshot.get95thPercentile(), NANOSECONDS, timeUnit),
      Percentile99Field -> convertToOtherTimeUnit(snapshot.get99thPercentile(), NANOSECONDS, timeUnit),
      Percentile999Field -> convertToOtherTimeUnit(snapshot.get999thPercentile(), NANOSECONDS, timeUnit),
      OneMinuteField -> convertToOtherTimeUnit(timer.getOneMinuteRate, SECONDS, timeUnit),
      FiveMinuteField -> convertToOtherTimeUnit(timer.getFiveMinuteRate, SECONDS, timeUnit),
      FifteenMinuteField -> convertToOtherTimeUnit(timer.getFifteenMinuteRate, SECONDS, timeUnit),
      MeanRateField -> convertToOtherTimeUnit(timer.getMeanRate, SECONDS, timeUnit),
      RunCountField -> timer.getCount
    ).map {
      case (key, value) => Field(key, value)
    }.toList
  }

  override def withFieldMapper(mapper: Field => Option[Field]): TimerCollector =
    new TimerCollector(timeUnit, staticTags, mapper)

  override def withStaticTags(tags: List[Tag]): TimerCollector =
    new TimerCollector(timeUnit, tags, fieldFM)

  private def convertToOtherTimeUnit(value: Double, oldTimeUnit: TimeUnit, newTimeUnit: TimeUnit): Double = {
    if (oldTimeUnit != newTimeUnit) Duration(value, oldTimeUnit).toUnit(newTimeUnit)
    else value
  }

}

object TimerCollector {
  val CountField = "count"
  val MinField = "min"
  val MaxField = "max"
  val MeanField = "mean"
  val StdDevField = "std-dev"
  val Percentile50Field = "50-percentile"
  val Percentile75Field = "75-percentile"
  val Percentile95Field = "95-percentile"
  val Percentile99Field = "99-percentile"
  val Percentile999Field = "999-percentile"
  val OneMinuteField = "one-minute"
  val FiveMinuteField = "five-minute"
  val FifteenMinuteField = "fifteen-minute"
  val MeanRateField = "mean-rate"
  val RunCountField = "run-count"

  def apply(timeUnit: TimeUnit): TimerCollector = new TimerCollector(timeUnit)
}

object SecondTimerCollector extends TimerCollector(SECONDS)

object MillisecondTimerCollector extends TimerCollector(MILLISECONDS)

object MinuteTimerCollector extends TimerCollector(MINUTES)

