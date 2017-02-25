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
import influxdbreporter.core._
import influxdbreporter.core.writers.LineProtocolWriter

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.collection.JavaConverters._

object HttpInfluxdbReporter {

  def default(registry: MetricRegistry)(implicit executionContext: ExecutionContext): Try[Reporter] = {
    val config = ConfigFactory.load().getConfig("metrics")
    default(config, registry)
  }

  def default(config: Config, registry: MetricRegistry)
             (implicit executionContext: ExecutionContext): Try[Reporter] = {
    // Can't use config.getDuration because of java 7 restriction
    for {
      interval <- Try(FiniteDuration(config.getDuration("interval").toMillis, TimeUnit.MILLISECONDS))
      reporter <- parseConfig(config).map { connectionData =>
        new InfluxdbReporter[String](
          registry,
          new LineProtocolWriter(getStaticTags(config)),
          new HttpInfluxdbClientFactory(connectionData)(executionContext, (interval * 9) / 10),
          interval,
          new InfluxBatcher,
          Try(config.getInt("unsent-buffer-size")).toOption.map(new FixedSizeWriterDataBuffer(_))
        )
      }
    } yield reporter
  }

  private def parseConfig(config: Config) = Try {
    ConnectionData(
      config.getString("address"),
      config.getInt("port"),
      config.getString("db-name"),
      config.getString("user"),
      config.getString("password")
    )
  }

  private def getStaticTags(config: Config): List[Tag] =
    Try {
      config.getConfigList("static-tags").asScala
        .map(c => Tag(c.getString("name"), c.getString("value")))
        .toList
    }.getOrElse(Nil)
}