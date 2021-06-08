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
package influxdbreporter;

import com.zaxxer.hikari.metrics.MetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;

import java.util.concurrent.TimeUnit;

import influxdbreporter.core.Tag;
import influxdbreporter.core.metrics.pull.PullingGauge;
import influxdbreporter.core.metrics.pull.ValueByTag;
import influxdbreporter.javawrapper.MetricRegistry;
import influxdbreporter.core.metrics.push.Histogram;
import influxdbreporter.core.metrics.push.Meter;
import influxdbreporter.core.metrics.push.Timer;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Future$;

public final class InfluxdbReporterMetricsTracker extends MetricsTracker {

    private final Timer connectionObtainTimer;
    private final Histogram connectionUsage;
    private final Meter connectionTimeoutMeter;
    private final MetricRegistry registry;

    private final String waitMetricName;
    private final String usageMetricName;
    private final String connectionTimeoutRateMetricName;
    private final String totalConnectionsMetricName;
    private final String idleConnectionsMetricName;
    private final String activeConnectionsMetricName;
    private final String pendingConnectionsMetricName;

    public InfluxdbReporterMetricsTracker(final String poolName, final PoolStats poolStats, final MetricRegistry registry) {
        this.registry = registry;

        this.waitMetricName =  poolName + ".pool.Wait";
        this.usageMetricName =  poolName + ".pool.Usage";
        this.connectionTimeoutRateMetricName =  poolName + ".pool.ConnectionTimeoutRate";
        this.totalConnectionsMetricName = poolName + ".pool.TotalConnections";
        this.idleConnectionsMetricName = poolName + ".pool.IdleConnections";
        this.activeConnectionsMetricName = poolName + ".pool.ActiveConnections";
        this.pendingConnectionsMetricName = poolName + ".pool.PendingConnections";

        this.connectionObtainTimer = registry.register(waitMetricName, new Timer());
        this.connectionUsage = registry.register(usageMetricName, new Histogram());
        this.connectionTimeoutMeter = registry.register(connectionTimeoutRateMetricName, new Meter());

        registry.register(totalConnectionsMetricName, new PullingGauge<Integer>() {
            @Override
            public Future<List<ValueByTag<Integer>>> getValues(ExecutionContext ec) {
                return convertToFutureOfValueByTagList(poolStats.getTotalConnections());
            }
        });

        registry.register(idleConnectionsMetricName, new PullingGauge<Integer>() {
            @Override
            public Future<List<ValueByTag<Integer>>> getValues(ExecutionContext ec) {
                return convertToFutureOfValueByTagList(poolStats.getIdleConnections());
            }
        });

        registry.register(activeConnectionsMetricName, new PullingGauge<Integer>() {
            @Override
            public Future<List<ValueByTag<Integer>>> getValues(ExecutionContext ec) {
                return convertToFutureOfValueByTagList(poolStats.getActiveConnections());
            }
        });

        registry.register(pendingConnectionsMetricName, new PullingGauge<Integer>() {
            @Override
            public Future<List<ValueByTag<Integer>>> getValues(ExecutionContext ec) {
                return convertToFutureOfValueByTagList(poolStats.getPendingThreads());
            }
        });
    }

    @Override
    public void close() {
        registry.unregister(waitMetricName);
        registry.unregister(usageMetricName);
        registry.unregister(connectionTimeoutRateMetricName);
        registry.unregister(totalConnectionsMetricName);
        registry.unregister(idleConnectionsMetricName);
        registry.unregister(activeConnectionsMetricName);
        registry.unregister(pendingConnectionsMetricName);
    }

    @Override
    public void recordConnectionAcquiredNanos(final long elapsedAcquiredNanos) {
        connectionObtainTimer.calculatedTime(elapsedAcquiredNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordConnectionUsageMillis(final long elapsedBorrowedMillis) {
        connectionUsage.update(elapsedBorrowedMillis);
    }

    @Override
    public void recordConnectionTimeout() {
        connectionTimeoutMeter.mark();
    }

    private Future<List<ValueByTag<Integer>>> convertToFutureOfValueByTagList(int value) {
        List<Tag> emptyList = JavaConverters.asScalaBufferConverter(new java.util.ArrayList<Tag>()).asScala().toList();
        java.util.ArrayList<ValueByTag<Integer>> valueByTagArrayList = new java.util.ArrayList<>();
        valueByTagArrayList.add(new ValueByTag<>(emptyList,value));
        List<ValueByTag<Integer>> valueByTagList = JavaConverters.asScalaBufferConverter(valueByTagArrayList).asScala().toList();
        return Future$.MODULE$.successful(valueByTagList);
    }
}