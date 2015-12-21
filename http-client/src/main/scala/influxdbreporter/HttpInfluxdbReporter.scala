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
package influxdbreporter

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import influxdbreporter.core.{InfluxdbReporter, LineProtocolWriter, MetricRegistry, Reporter}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object HttpInfluxdbReporter {

  def default(registry: MetricRegistry)(implicit executionContext: ExecutionContext): Reporter = {
    val config = ConfigFactory.load().getConfig("metrics")
    default(config, registry)
  }

  def default(config: Config, registry: MetricRegistry)
             (implicit executionContext: ExecutionContext): Reporter = {
    implicit val timeout = FiniteDuration(5, TimeUnit.SECONDS)
    new InfluxdbReporter[String](
      registry,
      LineProtocolWriter,
      new HttpInfluxdbClient(ConnectionData(
        config.getString("address"),
        config.getInt("port"),
        config.getString("db-name"),
        config.getString("user"),
        config.getString("password")
      )),
      FiniteDuration(config.getLong("intervalSeconds"), TimeUnit.SECONDS) // Can't use config.getDuration because of java 7 restriction
    )
  }
}