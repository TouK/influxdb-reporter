package influxdbreporter.core.metrics.pull

import com.codahale.metrics.Gauge
import influxdbreporter.core.metrics.Metric.CodehaleGauge
import influxdbreporter.core.metrics.MetricByTag.{InfluxdbTags, MetricByTags}
import influxdbreporter.core.metrics.{Metric, MetricByTag}

abstract class PullingGauge[V] extends Metric[CodehaleGauge[V]] {
  override def popMetrics: MetricByTags[CodehaleGauge[V]] =
    getValues.map {
      case ValueByTag(tags, value) =>
        MetricByTag[CodehaleGauge[V]](tags, new Gauge[V] {
          override def getValue: V = value
        })
    }

  def getValues: List[ValueByTag[V]]
}

case class ValueByTag[V](tags: InfluxdbTags, value: V)