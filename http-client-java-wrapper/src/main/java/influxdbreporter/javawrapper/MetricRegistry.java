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
import influxdbreporter.core.collectors.*;
import influxdbreporter.core.metrics.*;

public class MetricRegistry {

    final influxdbreporter.core.MetricRegistry scalaRegistry;

    public MetricRegistry(String prefix) {
        scalaRegistry = MetricRegistry$.MODULE$.apply(prefix);
    }

    public <U extends com.codahale.metrics.Metric, T extends Metric<U>> T register(final String name, final T metric) {
        return scalaRegistry.register(
                name,
                new RegisterMagnet<T>() {
                    @Override
                    public T apply(final String name, MetricRegistryImpl registryImpl) {
                        return registryImpl.registerMetricWithCollector(name, metric, (MetricCollector<U>) metricCollectorOfMetric(metric));
                    }
                });
    }

    public void unregister(String name) {
        scalaRegistry.unregister(name);
    }

    private MetricCollector<? extends com.codahale.metrics.Metric> metricCollectorOfMetric(Metric metric) {
        if (metric instanceof Counter) return CounterCollector$.MODULE$;
        else if (metric instanceof Histogram) return HistogramCollector$.MODULE$;
        else if (metric instanceof Meter) return MeterCollector$.MODULE$;
        else if (metric instanceof Timer) return SecondTimerCollector$.MODULE$;
        else throw new IllegalArgumentException("Unknown metric type: " + metric.getClass().getName());
    }
}