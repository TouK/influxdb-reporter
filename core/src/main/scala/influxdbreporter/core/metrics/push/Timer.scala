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
package influxdbreporter.core.metrics.push

import java.util.concurrent.TimeUnit

import com.codahale.metrics.Clock
import influxdbreporter.core.Tag
import influxdbreporter.core.metrics.Metric._

import scala.annotation.varargs
import scala.language.postfixOps

sealed trait TimerContext {

  @varargs def stop(tags: Tag*): Unit = {
    stopWithTags(tags)
  }

  // cause of: https://issues.scala-lang.org/browse/SI-9013, https://issues.scala-lang.org/browse/SI-1459
  protected def stopWithTags(tags: Seq[Tag]): Unit
}

class Timer(clock: Clock) extends TagRelatedPushingMetric[CodahaleTimer] {

  def this() = this(Clock.defaultClock())

  @varargs def time(tags: Tag*): TimerContext = new InfluxTimerContextImpl(tags.toList, this, clock)

  @varargs def calculatedTime(time: Long, unit: TimeUnit, tags: Tag*): Unit = {
    increaseMetric(tags.toList, _.update(time, unit))
  }

  override protected def createMetric(): CodahaleTimer = new CodahaleTimer()

  private def notify(tags: List[Tag], time: Long): Unit =
    increaseMetric(tags, _.update(time, TimeUnit.NANOSECONDS))

  private class InfluxTimerContextImpl(startingTags: List[Tag], listener: Timer, clock: Clock)
    extends TimerContext {

    private val startTime: Long = clock.getTick

    override protected def stopWithTags(tags: Seq[Tag]): Unit = {
      listener.notify(
        tags.toList ::: startingTags,
        clock.getTick - startTime
      )
    }
  }

}
