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

import java.util.concurrent.TimeUnit

import influxdbreporter.core.writers.LineProtocolWriter

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait TestReporterProvider {

  protected def createReporter(metricsClient: MetricClient[String],
                               metricsRegistry: MetricRegistry = MetricRegistry("simple"),
                               buffer: Option[WriterDataBuffer[String]] = None)
                              (implicit executionContext: ExecutionContext) = {
    new InfluxdbReporter(metricsRegistry,
      new LineProtocolWriter,
      metricsClient,
      FiniteDuration(500, TimeUnit.MILLISECONDS),
      new SimpleBatcher(5),
      buffer
    )
  }
}