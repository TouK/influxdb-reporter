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

import influxdbreporter.core.metrics.push.Counter

import scala.concurrent.duration.FiniteDuration

class CounterTests extends BaseMetricTest {

  "An Counter metric integration test" in {
    val registry = MetricRegistry("test")
    val registeredCounter = registry.registerOrGetRegistered("mycounter", new Counter)

    val mockWriter = createMockWriter(onPhaseChange(registeredCounter), assertPhase)
    val mockClient = createMockMetricClientFactory(mockWriter)
    val reporter = new InfluxdbReporter(registry, mockWriter, mockClient, FiniteDuration(1, TimeUnit.SECONDS))
    val task = reporter.start()

    mockWriter.waitToPhaseThreeEnds()
    task.stop()
  }

  private def phaseOneTags = List(
    Set(Tag("strength", 1)),
    Set(Tag("strength", 2)),
    Set(Tag("strength", 1), Tag("strength", 2)),
    Set(Tag("strength", 2))
  )
  private def phaseTwoTags = List(
    Set(Tag("strength", 1))
  )
  private def phaseThreeTags = List(
    Set(Tag("strength", 2), Tag("strength", 1)),
    Set(Tag("strength", 1), Tag("strength", 2))
  )

  private def onPhaseChange(registeredCounter: Counter): PhaseChangeAction = phase =>
    phaseTags(phase).foreach { tags =>
      registeredCounter.inc(tags.toList: _*)
    }

  private def assertPhase: PhaseAssert = (phase, measurement, fields, tags) =>
    validateNameAndPhaseTagsAndCounts(measurement, phaseTags(phase), tags, fields)

  private def phaseTags: Phase => List[Set[Tag]] = {
    case PhaseOne => phaseOneTags
    case PhaseTwo => phaseTwoTags
    case PhaseThree => phaseThreeTags
  }

  private def validateNameAndPhaseTagsAndCounts(measurement: String,
                                                phaseTags: List[Set[Tag]],
                                                tags: Set[Tag],
                                                fields: List[Field]): Boolean =
    measurement.contains("mycounter") && phaseTags.contains(tags) && checkCount(fields, tags, phaseTags)

  private def checkCount(fields: List[Field], tags: Set[Tag], phaseTags: List[Set[Tag]]) =
    fields.exists(f => f.key == "count" && phaseTags.contains(tags))

}