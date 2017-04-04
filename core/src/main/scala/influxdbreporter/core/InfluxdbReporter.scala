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
package influxdbreporter.core

import com.codahale.metrics.Clock
import influxdbreporter.core.writers.Writer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class InfluxdbReporter[S](registry: MetricRegistry,
                          writer: Writer[S],
                          clientFactory: MetricClientFactory[S],
                          interval: FiniteDuration,
                          batcher: Batcher[S] = new InfluxBatcher[S],
                          buffer: Option[WriterDataBuffer[S]] = None,
                          clock: Clock = Clock.defaultClock())
                         (implicit executionContext: ExecutionContext)
  extends ScheduledReporter[S](registry, interval, writer, clientFactory, batcher, buffer, clock) {

  def withInterval(newInterval: FiniteDuration): InfluxdbReporter[S] =
    new InfluxdbReporter[S](registry, writer, clientFactory, newInterval, batcher, buffer, clock)

}