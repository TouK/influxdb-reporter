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
package influxdbreporter.javawrapper.collectors;

import com.codahale.metrics.Gauge;

import javax.annotation.Nonnull;

public class GaugeCollector<T> implements MetricCollector<Gauge<T>> {

    public static <T> influxdbreporter.core.collectors.GaugeCollector<T> collector() {
        return influxdbreporter.core.collectors.GaugeCollector.apply();
    }

    private final FieldMapperConverter converter;

    public GaugeCollector(@Nonnull FieldMapper mapper) {
        this.converter = new FieldMapperConverter(mapper);
    }

    public influxdbreporter.core.collectors.GaugeCollector<T> convertToScalaCollector() {
        return influxdbreporter.core.collectors.GaugeCollector
                .<T>apply()
                .withFieldMapper(converter.mapperToScalaFunction1());
    }
}