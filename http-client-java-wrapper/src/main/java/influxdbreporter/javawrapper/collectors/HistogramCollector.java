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

import com.codahale.metrics.Histogram;

import javax.annotation.Nonnull;

public class HistogramCollector implements MetricCollector<Histogram> {

    public static final influxdbreporter.core.collectors.HistogramCollector COLLECTOR =
            influxdbreporter.core.collectors.HistogramCollector.apply();

    private final FieldMapperConverter converter;

    public HistogramCollector(@Nonnull FieldMapper mapper) {
        this.converter = new FieldMapperConverter(mapper);
    }

    public influxdbreporter.core.collectors.HistogramCollector convertToScalaCollector() {
        return COLLECTOR.withFieldMapper(converter.mapperToScalaFunction1());
    }

    scala.Int
}