package influxdbreporter.javawrapper.collectors;

public interface MetricCollector<U extends com.codahale.metrics.Metric> {
    influxdbreporter.core.collectors.MetricCollector<U> convertToScalaCollector();
}
