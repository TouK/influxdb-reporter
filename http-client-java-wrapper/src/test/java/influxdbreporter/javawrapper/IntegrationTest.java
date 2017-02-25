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

import influxdbreporter.ConnectionData;
import influxdbreporter.core.MetricClient;
import influxdbreporter.core.MetricClientFactory;
import influxdbreporter.core.StoppableReportingTask;
import influxdbreporter.core.Tag;
import influxdbreporter.core.metrics.push.Timer;
import influxdbreporter.core.metrics.push.TimerContext;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class IntegrationTest {

    @Test
    public void metricRegistryShouldBeAbleToHandleTimerMetricTest() {
        MetricRegistry registry = new MetricRegistry("test-registry");
        InfluxdbReporter reporter = createTestReporter(registry);

        Timer t1 = new Timer();
        registry.register("t1", t1);

        StoppableReportingTask task = reporter.start();

        TimerContext t1Context = t1.time();
        t1Context.stop();

        TimerContext t2Context = t1.time();
        t2Context.stop(new Tag("t1", 1), new Tag("t2", 2));

        registry.unregister("t1");
        task.stop();
    }

    private InfluxdbReporter createTestReporter(MetricRegistry registry) {
        final influxdbreporter.HttpInfluxdbClient client = HttpInfluxdbClient.defaultHttpClient(
                new ConnectionData("addr", 2000, "db", "user", "pass")
        );
        MetricClientFactory<String> metricClientFactory = new MetricClientFactory<String>() {
            @Override
            public MetricClient<String> create() {
                return client;
            }
        };
        return new InfluxdbReporter(registry, metricClientFactory, 10, TimeUnit.SECONDS);
    }
}