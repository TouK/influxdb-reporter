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

import com.codahale.metrics.ExponentiallyDecayingReservoir
import influxdbreporter.core.metrics.Metric.CodahaleHistogram

import scala.concurrent.duration.FiniteDuration

class CodehaleHistogramTests extends BaseMetricTest {

  "A CodehaleHistogram metric integration test" in {
    val registry = MetricRegistry("test")
    val registeredHistogram = registry.register("myhistogram", new CodahaleHistogram(new ExponentiallyDecayingReservoir))

    val mockWriter = createMockWriter(onPhaseChange(registeredHistogram), assertPhase)
    val mockClient = createMockMetricClient(mockWriter)
    val reporter = new InfluxdbReporter(registry, mockWriter, mockClient, FiniteDuration(1, TimeUnit.SECONDS))
    val task = reporter.start()

    mockWriter.waitToPhaseThreeEnds()
    task.stop()
  }

  private def onPhaseChange(registeredHistogram: CodahaleHistogram): PhaseChangeAction = {
    case PhaseOne =>
      registeredHistogram.update(3)
      registeredHistogram.update(1)
    case PhaseTwo =>
      registeredHistogram.update(5)
    case PhaseThree =>
      registeredHistogram.update(2)
      registeredHistogram.update(1)
      registeredHistogram.update(0)
  }

  private def assertPhase: PhaseAssert = (phase, _, fields, tags) => phase match {
    case PhaseOne =>
      checkFieldsValues(fields, ExpectedValues(2, 3, 1, 3)) && tags.isEmpty
    case PhaseTwo =>
      checkFieldsValues(fields, ExpectedValues(3, 3, 1, 5)) && tags.isEmpty
    case PhaseThree =>
      checkFieldsValues(fields, ExpectedValues(6, 1, 0, 5)) && tags.isEmpty
  }

  private def checkFieldsValues(fields: List[Field], expectedValues: ExpectedValues) =
    Map(
      "run-count" -> expectedValues.runCount,
      "median" -> expectedValues.median,
      "min" -> expectedValues.min,
      "max" -> expectedValues.max
    ).forall {
      case (key, value) => fields.exists(f => f.key == key && f.value == value)
    }

  case class ExpectedValues(runCount: Int, median: Double, min: Int, max: Int)

}
