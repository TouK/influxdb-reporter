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
package influxdbreporter.core

import java.util.concurrent.TimeUnit

import com.codahale.metrics.Timer
import influxdbreporter.core.collectors.TimerCollector._
import influxdbreporter.core.collectors.{SecondTimerCollector, TimerCollector}
import influxdbreporter.core.writers.Writer
import org.scalatest.WordSpec
import org.scalamock.scalatest.MockFactory

class TimerCollectorTests extends WordSpec with MockFactory {

  val writerMock = mock[Writer[String]]
  val name = "test"
  val measurementName = s"$name.timer"
  val timestamp = 10000000L
  val tagList = new Tag("key1", 1) :: Nil
  val timerFields = List(
    fieldD(OneMinuteField), fieldI(CountField), fieldD(Percentile50Field), fieldD(Percentile75Field), fieldD(MeanField),
    fieldD(MinField), fieldI(RunCountField), fieldD(MaxField), fieldD(Percentile99Field), fieldD(Percentile95Field),
    fieldD(Percentile999Field), fieldD(StdDevField), fieldD(FifteenMinuteField), fieldD(FiveMinuteField), fieldD(MeanRateField)
  )

  "A TimerCollector" should {
    "write collector specific fields" in {
      (writerMock.write(_: String, _: List[Field], _: List[Tag], _: Long))
        .expects(measurementName, timerFields, tagList, timestamp)
      SecondTimerCollector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
    }

    "write collector specific fields and static tags" in {
      val staticTags = Tag("st1", "static tag") :: Nil
      val timerCollector = new TimerCollector(TimeUnit.SECONDS, staticTags)
      (writerMock.write(_: String, _: List[Field], _: List[Tag], _: Long))
        .expects(measurementName, timerFields, tagList ::: staticTags , timestamp)
      timerCollector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
    }

    "write filtered list of fields when collector was properly configured" in {
      val removedFieldKeys = Percentile50Field :: Percentile75Field :: Percentile95Field :: Percentile99Field :: Percentile999Field :: Nil
      val filteredFields = timerFields.filter(f => !removedFieldKeys.contains(f.key))
      val collector = SecondTimerCollector.withFieldMapper { field =>
        if (removedFieldKeys.contains(field.key)) {
          None
        } else {
          Some(field)
        }
      }
      (writerMock.write(_: String, _: List[Field], _: List[Tag], _: Long))
        .expects(measurementName, filteredFields, tagList, timestamp)

      collector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
    }

    "write fields with changed field name and value when collector was properly configured" in {
      val collector = SecondTimerCollector.withFieldMapper { field =>
        if (field.key == RunCountField) {
          val value = field.value match {
            case i: Number => i.doubleValue()
            case _ => 0.0
          }
          Some(Field(field.key, value))
        } else {
          Some(field)
        }
      }

      (writerMock.write(_: String, _: List[Field], _: List[Tag], _: Long))
        .expects(where {
          case (`measurementName`, fields, `tagList`, `timestamp`) =>
            fields.find(_.key == RunCountField).exists(_.value.isInstanceOf[Double])
          case _ => false
        })

      collector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
    }

    "return None when all fields was filtered" in {
      val collector = SecondTimerCollector.withFieldMapper(_ => None)
      assertResult(None) {
        collector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
      }
    }
  }

  private def fieldD(key: String) = new Field(key, 0.0)

  private def fieldI(key: String) = new Field(key, 0)
}