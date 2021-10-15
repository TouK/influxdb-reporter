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

import com.codahale.metrics.Clock;
import influxdbreporter.core.*;
import influxdbreporter.core.writers.LineProtocolWriter;
import scala.Option;
import scala.Some;
import scala.collection.immutable.List$;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class InfluxdbReporter {

    private final influxdbreporter.core.InfluxdbReporter<String> reporter;


    public InfluxdbReporter(MetricRegistry registry,
                            MetricClientFactory<String> clientFactory,
                            long reportInterval,
                            TimeUnit reportIntervalUnit)  {
        this(registry, clientFactory, reportInterval, reportIntervalUnit, 0, Optional.empty());
    }

    public InfluxdbReporter(MetricRegistry registry,
                            MetricClientFactory<String> clientFactory,
                            long reportInterval,
                            TimeUnit reportIntervalUnit,
                            int bufferSize)  {
        this(registry, clientFactory, reportInterval, reportIntervalUnit, bufferSize, Optional.empty());
    }

    public InfluxdbReporter(MetricRegistry registry,
                            MetricClientFactory<String> clientFactory,
                            long reportInterval,
                            TimeUnit reportIntervalUnit,
                            int bufferSize,
                            String name) {
        this(registry, clientFactory, reportInterval, reportIntervalUnit, bufferSize, Optional.of(name));
    }

    private InfluxdbReporter(MetricRegistry registry,
                            MetricClientFactory<String> clientFactory,
                            long reportInterval,
                            TimeUnit reportIntervalUnit,
                            int bufferSize,
                            Optional<String> name) {
        WriterDataBuffer<String> dataBuffer = new FixedSizeWriterDataBuffer<>(bufferSize);

        reporter = new influxdbreporter.core.InfluxdbReporter<>(
                registry.scalaRegistry,
                new LineProtocolWriter(List$.MODULE$.<Tag>empty()),
                clientFactory,
                FiniteDuration.apply(reportInterval, reportIntervalUnit),
                new InfluxBatcher<String>(),
                new Some<>(dataBuffer),
                Clock.defaultClock(),
                name.isPresent() ? Option.empty() : Option.apply(name.get()),
                ExecutionContext.Implicits$.MODULE$.global()
        );
    }

    public StoppableReportingTask start() {
        return reporter.start();
    }

}