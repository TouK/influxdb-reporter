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

import java.util.concurrent.TimeUnit

import influxdbreporter.core.metrics.Metric.CodehaleTimer
import influxdbreporter.core.{Field, Tag, Writer, WriterData}

import scala.concurrent.duration._

class TimerCollector(timeUnit: TimeUnit) extends MetricCollector[CodehaleTimer] {

  override def collect[U](writer: Writer[U], name: String, metric: CodehaleTimer, timestamp: Long, tags: Tag*): WriterData[U] =
    writer.write(s"$name.timer", fields(metric), tags.toList, timestamp)

  private def fields(timer: CodehaleTimer): List[Field] = {
    val snapshot = timer.getSnapshot
    Map(
      "count" -> snapshot.size,
      "min" -> convertToOtherTimeUnit(snapshot.getMin, NANOSECONDS, timeUnit),
      "max" -> convertToOtherTimeUnit(snapshot.getMax, NANOSECONDS, timeUnit),
      "mean" -> convertToOtherTimeUnit(snapshot.getMean, NANOSECONDS, timeUnit),
      "std-dev" -> convertToOtherTimeUnit(snapshot.getStdDev, NANOSECONDS, timeUnit),
      "50-percentile" -> convertToOtherTimeUnit(snapshot.getMedian, NANOSECONDS, timeUnit),
      "75-percentile" -> convertToOtherTimeUnit(snapshot.get75thPercentile(), NANOSECONDS, timeUnit),
      "95-percentile" -> convertToOtherTimeUnit(snapshot.get95thPercentile(), NANOSECONDS, timeUnit),
      "99-percentile" -> convertToOtherTimeUnit(snapshot.get99thPercentile(), NANOSECONDS, timeUnit),
      "999-percentile" -> convertToOtherTimeUnit(snapshot.get999thPercentile(), NANOSECONDS, timeUnit),
      "one-minute" -> convertToOtherTimeUnit(timer.getOneMinuteRate, SECONDS, timeUnit),
      "five-minute" -> convertToOtherTimeUnit(timer.getFiveMinuteRate, SECONDS, timeUnit),
      "fifteen-minute" -> convertToOtherTimeUnit(timer.getFifteenMinuteRate, SECONDS, timeUnit),
      "mean-rate" -> convertToOtherTimeUnit(timer.getMeanRate, SECONDS, timeUnit),
      "run-count" -> timer.getCount
    ).map {
      case (key, value) => Field(key, value)
    }.toList
  }

  private def convertToOtherTimeUnit(value: Double, oldTimeUnit: TimeUnit, newTimeUnit: TimeUnit): Double = {
    if (oldTimeUnit != newTimeUnit) Duration(value, oldTimeUnit).toUnit(newTimeUnit)
    else value
  }
}

object SecondTimerCollector extends TimerCollector(SECONDS)

object MillisecondTimerCollector extends TimerCollector(MILLISECONDS)

object MinuteTimerCollector extends TimerCollector(MINUTES)

