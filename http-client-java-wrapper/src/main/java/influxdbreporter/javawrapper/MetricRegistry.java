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
package influxdbreporter.javawrapper;

import influxdbreporter.core.MetricRegistry$;
import influxdbreporter.core.MetricRegistryImpl;
import influxdbreporter.core.RegisterMagnet;
import influxdbreporter.core.metrics.Metric;
import influxdbreporter.core.metrics.pull.PullingGauge;
import influxdbreporter.core.metrics.push.*;
import influxdbreporter.javawrapper.collectors.*;

public class MetricRegistry {

  final influxdbreporter.core.MetricRegistry scalaRegistry;

  public MetricRegistry(String prefix) {
    scalaRegistry = MetricRegistry$.MODULE$.apply(prefix);
  }

  public MetricRegistry(influxdbreporter.core.MetricRegistry scalaRegistry) {
    this.scalaRegistry = scalaRegistry;
  }

  public <U extends com.codahale.metrics.Metric, T extends Metric<U>> T register(final String name, final T metric) {
    return scalaRegistry.register(
        name,
        new RegisterMagnet<T>() {
          @Override
          public T register(String metricName, MetricRegistryImpl registryImpl) {
            return registryImpl.registerMetricWithCollector(
                name,
                metric,
                (influxdbreporter.core.collectors.MetricCollector<U>) metricCollectorOfMetric(metric)
            );
          }

          @Override
          public T registerOrGetRegistered(String metricName, MetricRegistryImpl registryImpl) {
            return registryImpl.registerMetricWithCollector(
                name,
                metric,
                (influxdbreporter.core.collectors.MetricCollector<U>) metricCollectorOfMetric(metric)
            );
          }
        });
  }

  public <U extends com.codahale.metrics.Metric, T extends Metric<U>> T register(final String name,
                                                                                 final T metric,
                                                                                 final MetricCollector<U> collector) {
    return scalaRegistry.register(
        name,
        new RegisterMagnet<T>() {
          @Override
          public T register(String metricName, MetricRegistryImpl registryImpl) {
            return registryImpl.registerMetricWithCollector(
                name,
                metric,
                collector.convertToScalaCollector()
            );
          }

          @Override
          public T registerOrGetRegistered(String metricName, MetricRegistryImpl registryImpl) {
            return registryImpl.registerMetricWithCollector(
                name,
                metric,
                collector.convertToScalaCollector()
            );
          }
        });
  }

  public void unregister(String name) {
    scalaRegistry.unregister(name);
  }

  private influxdbreporter.core.collectors.MetricCollector<? extends com.codahale.metrics.Metric> metricCollectorOfMetric(Metric metric) {
    if (metric instanceof Counter) return CounterCollector.COLLECTOR;
    else if (metric instanceof Histogram) return HistogramCollector.COLLECTOR;
    else if (metric instanceof Meter) return MeterCollector.COLLECTOR;
    else if (metric instanceof Timer) return TimerCollector.COLLECTOR;
    else if (metric instanceof DiscreteGauge) return GaugeCollector.collector();
    else if (metric instanceof PullingGauge) return GaugeCollector.collector();
    else throw new IllegalArgumentException("Unknown metric type: " + metric.getClass().getName());
  }
}