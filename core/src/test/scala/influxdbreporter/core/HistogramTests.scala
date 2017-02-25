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

import influxdbreporter.core.metrics.push.Histogram

import scala.concurrent.duration.FiniteDuration

class HistogramTests extends BaseMetricTest {

  "An Histogram metric integration test" in {
    val registry = MetricRegistry("test")
    val registeredHistogram = registry.register("myhistogram", new Histogram())

    val mockWriter = createMockWriter(onPhaseChange(registeredHistogram), assertPhase)
    val mockClient = createMockMetricClientFactory(mockWriter)
    val reporter = new InfluxdbReporter(registry, mockWriter, mockClient, FiniteDuration(1, TimeUnit.SECONDS))
    val task = reporter.start()

    mockWriter.waitToPhaseThreeEnds()
    task.stop()
  }

  private val tag1 = Tag("tag", 1)
  private val tag2 = Tag("tag", 2)

  private def onPhaseChange(registeredHistogram: Histogram): PhaseChangeAction = {
    case PhaseOne =>
      registeredHistogram.update(3, tag1, tag2)
      registeredHistogram.update(2, tag2, tag1)
      registeredHistogram.update(1, tag1)
      registeredHistogram.update(2)
    case PhaseTwo =>
      registeredHistogram.update(5, tag2)
      registeredHistogram.update(2, tag2)
      registeredHistogram.update(2, tag1)
    case PhaseThree =>
      registeredHistogram.update(4, tag1, tag2)
      registeredHistogram.update(5, tag2, tag1)
      registeredHistogram.update(3, tag1)
      registeredHistogram.update(1)
      registeredHistogram.update(1)
  }

  private def assertPhase: PhaseAssert = (phase, _, fields, tags) => phase match {
    case PhaseOne => tags match {
      case set if set == Set(tag1, tag2) => checkRunCount(fields, 2)
      case set if set == Set(tag1) => checkRunCount(fields, 1)
      case set if set.isEmpty => checkRunCount(fields, 1)
    }
    case PhaseTwo => tags match {
      case set if set == Set(tag2) => checkRunCount(fields, 2)
      case set if set == Set(tag1) => checkRunCount(fields, 1)
    }
    case PhaseThree => tags match {
      case set if set == Set(tag1, tag2) => checkRunCount(fields, 2)
      case set if set == Set(tag1) => checkRunCount(fields, 1)
      case set if set.isEmpty => checkRunCount(fields, 2)
    }
  }

  private def checkRunCount(fields: List[Field], expectedRunCount: Int) =
    fields.exists(f => f.key == "run-count" && f.value == expectedRunCount)

  case class ExpectedValues(runCount: Int, median: Double, min: Int, max: Int)

}
