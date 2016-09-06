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
import influxdbreporter.core.StoppableReportingTask;
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

        registry.unregister("t1");
        task.stop();
    }

    private InfluxdbReporter createTestReporter(MetricRegistry registry) {
        influxdbreporter.HttpInfluxdbClient client = HttpInfluxdbClient.defaultHttpClient(
                new ConnectionData("addr", 2000, "db", "user", "pass")
        );
        return new InfluxdbReporter(registry, client, 10, TimeUnit.SECONDS);
    }
}