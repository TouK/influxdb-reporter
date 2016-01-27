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

import influxdbreporter.core.metrics.pull.{PullingGauge, ValueByTag}

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration

class PullingGaugeTests extends BaseMetricTest{
  "A TagRelatedPullingMetricTests metric integration test" in {
    val registry = MetricRegistry("test")
    var valueByTag = ValueByTag(List.empty, -1)
    val registeredCounter = registry.register("mygauge", new PullingGauge[Int] {
      override def getValues(implicit ec: ExecutionContext): Future[List[ValueByTag[Int]]] =
        Future.successful(List(valueByTag))
    })
    def changeValue(tags: List[Tag], v: Int) = {
      valueByTag = ValueByTag(tags, v)
    }

    val mockWriter = createMockWriter(onPhaseChange(changeValue), assertPhase)
    val mockClient = createMockMetricClient(mockWriter)
    val reporter = new InfluxdbReporter(registry, mockWriter, mockClient, FiniteDuration(1, TimeUnit.SECONDS))
    val task = reporter.start()

    mockWriter.waitToPhaseThreeEnds()
    task.stop()
  }

  private def onPhaseChange(changeValue: (List[Tag], Int) => Unit): PhaseChangeAction = {
    case PhaseOne =>
      changeValue(Tag("tag1", 2) :: Nil, 1)
    case PhaseTwo =>
      changeValue(Tag("tag1", 4) :: Nil, 3)
    case PhaseThree =>
      changeValue(Tag("tag1", 6) :: Nil, 5)
  }

  private def assertPhase: PhaseAssert = (phase, _, fields, tags) => phase match {
    case PhaseOne => checkValueEquals(fields, 1) && tags == List(Tag("tag1", 2))
    case PhaseTwo => checkValueEquals(fields, 3) && tags == List(Tag("tag1", 4))
    case PhaseThree => checkValueEquals(fields, 5) && tags == List(Tag("tag1", 6))
  }

  private def checkValueEquals(fields: List[Field], expectedCount: Int) =
    fields.exists(f => f.key == "value" && f.value == expectedCount)
}