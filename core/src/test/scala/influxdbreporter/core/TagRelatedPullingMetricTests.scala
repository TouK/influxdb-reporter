package influxdbreporter.core

import java.util.concurrent.TimeUnit

import influxdbreporter.core.metrics.pull.{PullingGauge, ValueByTag}

import scala.concurrent.duration.FiniteDuration

class TagRelatedPullingMetricTests extends BaseMetricTest{
  "A TagRelatedPullingMetricTests metric integration test" in {
    val registry = MetricRegistry("test")
    var valueByTag = ValueByTag(List.empty, -1)
    val registeredCounter = registry.register("mygauge", new PullingGauge[Int] {
      override def getValues: List[ValueByTag[Int]] = List(valueByTag)
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
