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
package influxdbreporter.core.metrics

import com.codahale.metrics.{Reservoir, ExponentiallyDecayingReservoir}
import Metric.CodehaleHistogram
import influxdbreporter.core.Tag

import scala.annotation.varargs


class Histogram(reservoir: Reservoir) extends TagRelatedMetric[CodehaleHistogram] with Metric[CodehaleHistogram]{
  def this() = this(new ExponentiallyDecayingReservoir)

  override protected def createMetric(): CodehaleHistogram = new CodehaleHistogram(reservoir)

  @varargs def update(n: Long, tags: Tag*): Unit = increaseMetric(tags.toList, _.update(n))
}
