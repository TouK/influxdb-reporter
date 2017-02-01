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
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.mockito.Mockito.verify
import org.scalatest.WordSpec
import org.scalatest.mockito.MockitoSugar

class TimerCollectorTests extends WordSpec with MockitoSugar {

  val name = "test"
  val measurementName = s"$name.timer"
  val timestamp = 10000000L
  val tagList = Tag("key1", 1) :: Nil
  val tagSet = tagList.toSet
  val timerFields = List(
    fieldD(OneMinuteField), fieldI(CountField), fieldD(Percentile50Field), fieldD(Percentile75Field), fieldD(MeanField),
    fieldD(MinField), fieldI(RunCountField), fieldD(MaxField), fieldD(Percentile99Field), fieldD(Percentile95Field),
    fieldD(Percentile999Field), fieldD(StdDevField), fieldD(FifteenMinuteField), fieldD(FiveMinuteField), fieldD(MeanRateField)
  )

  "A TimerCollector" should {
    "write collector specific fields" in {
      val writerMock = Mockito.mock(classOf[Writer[String]])
      SecondTimerCollector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
      verify(writerMock).write(measurementName, timerFields, tagList.toSet[Tag], timestamp)
    }

    "write collector specific fields and static tags" in {
      val writerMock = mock[Writer[String]]
      val staticTags = Tag("st1", "static tag") :: Nil
      val timerCollector = new TimerCollector(TimeUnit.SECONDS, staticTags)
      timerCollector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
      verify(writerMock).write(measurementName, timerFields, tagList.toSet ++ staticTags , timestamp)
    }

    "write filtered list of fields when collector was properly configured" in {
      val writerMock = mock[Writer[String]]
      val removedFieldKeys = Percentile50Field :: Percentile75Field :: Percentile95Field :: Percentile99Field :: Percentile999Field :: Nil
      val filteredFields = timerFields.filter(f => !removedFieldKeys.contains(f.key))
      val collector = SecondTimerCollector.withFieldMapper { field =>
        if (removedFieldKeys.contains(field.key)) {
          None
        } else {
          Some(field)
        }
      }
      collector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
      verify(writerMock).write(measurementName, filteredFields, tagList.toSet[Tag], timestamp)
    }

    "write fields with changed field name and value when collector was properly configured" in {
      val writerMock = mock[Writer[String]]
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

      collector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
      val fieldsCaptor: ArgumentCaptor[List[Field]] = ArgumentCaptor.forClass(classOf[List[Field]])
      val tagsCaptor: ArgumentCaptor[Set[Tag]] = ArgumentCaptor.forClass(classOf[Set[Tag]])

      verify(writerMock).write(ArgumentMatchers.anyString(), fieldsCaptor.capture(),
        tagsCaptor.capture(), ArgumentMatchers.anyLong())

      assert(fieldsCaptor.getValue.find(_.key == RunCountField).exists(_.value.isInstanceOf[Double]))
    }

    "return None when all fields was filtered" in {
      val writerMock = mock[Writer[String]]
      val collector = SecondTimerCollector.withFieldMapper(_ => None)
      assertResult(None) {
        collector.collect(writerMock, name, new Timer, timestamp, tagList: _*)
      }
    }
  }

  private def fieldD(key: String) = Field(key, 0.0)

  private def fieldI(key: String) = Field(key, 0)
}