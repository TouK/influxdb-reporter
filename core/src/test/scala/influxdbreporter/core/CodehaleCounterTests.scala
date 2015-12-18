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

import influxdbreporter.core.metrics.Metric.CodehaleCounter

import scala.concurrent.duration.FiniteDuration

class CodehaleCounterTests extends BaseMetricTest {

  "A CodehaleCounter metric integration test" in {
    val registry = MetricRegistry("test")
    val registeredCounter = registry.register("mycounter", new CodehaleCounter)

    val mockWriter = createMockWriter(onPhaseChange(registeredCounter), assertPhase)
    val mockClient = createMockMetricClient(mockWriter)
    val reporter = new InfluxdbReporter(registry, mockWriter, mockClient, FiniteDuration(1, TimeUnit.SECONDS))
    val task = reporter.start()

    mockWriter.waitToPhaseThreeEnds()
    task.stop()
  }

  private def onPhaseChange(registeredCounter: CodehaleCounter): PhaseChangeAction = {
    case PhaseOne =>
      registeredCounter.inc()
      registeredCounter.inc()
    case PhaseTwo =>
      registeredCounter.inc(4)
    case PhaseThree =>
      registeredCounter.inc()
  }

  private def assertPhase: PhaseAssert = (phase, _, fields, tags) => phase match {
    case PhaseOne => checkCountEquals(fields, 2) && tags.isEmpty
    case PhaseTwo => checkCountEquals(fields, 6) && tags.isEmpty
    case PhaseThree => checkCountEquals(fields, 7) && tags.isEmpty
  }

  private def checkCountEquals(fields: List[Field], expectedCount: Int) =
    fields.exists(f => f.key == "count" && f.value == expectedCount)
}
