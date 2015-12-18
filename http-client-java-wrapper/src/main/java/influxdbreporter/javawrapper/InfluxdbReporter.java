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

import influxdbreporter.HttpInfluxdbClient;
import influxdbreporter.core.LineProtocolWriter$;
import influxdbreporter.core.StoppableReportingTask;
import influxdbreporter.core.utils.UtcClock$;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class InfluxdbReporter {

    private final influxdbreporter.core.InfluxdbReporter<String> reporter;

    public InfluxdbReporter(MetricRegistry registry, InfluxdbConnectionData connectionData, long interval, TimeUnit unit) {
        HttpInfluxdbClient client = new HttpInfluxdbClient(connectionData.address, connectionData.port,
                connectionData.dbName, connectionData.user, connectionData.password,
                ExecutionContext.Implicits$.MODULE$.global());
        reporter = new influxdbreporter.core.InfluxdbReporter<>(registry.scalaRegistry,
                LineProtocolWriter$.MODULE$, client, FiniteDuration.apply(interval, unit), UtcClock$.MODULE$);
    }

    public StoppableReportingTask start() {
        return reporter.start();
    }

    public static class InfluxdbConnectionData {
        private final String address;
        private final int port;
        private final String dbName;
        private final String user;
        private final String password;

        public InfluxdbConnectionData(String address, int port, String dbName, String user, String password) {
            this.address = address;
            this.port = port;
            this.dbName = dbName;
            this.user = user;
            this.password = password;
        }

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public String getDbName() {
            return dbName;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }
    }
}