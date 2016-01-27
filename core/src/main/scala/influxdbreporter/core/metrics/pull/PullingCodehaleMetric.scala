package influxdbreporter.core.metrics.pull

import influxdbreporter.core.metrics.{MetricByTag, Metric}
import influxdbreporter.core.metrics.Metric.CodehaleMetric
import influxdbreporter.core.metrics.MetricByTag.MetricByTags

class PullingCodehaleMetric[T <: CodehaleMetric](underlying: T) extends Metric[T] {

  override def popMetrics: MetricByTags[T] = {
    List(MetricByTag(List.empty, underlying))
  }

}