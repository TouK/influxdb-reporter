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

import influxdbreporter.core.Tag
import influxdbreporter.core.metrics.Metric.CodehaleMeter

import scala.annotation.varargs

class Meter extends TagRelatedMetric[CodehaleMeter] {

  override protected def createMetric(): CodehaleMeter = new CodehaleMeter()

  @varargs def mark(tags: Tag*): Unit = mark(1L, tags: _*)

  @varargs def mark(n: Long,tags: Tag*): Unit = increaseMetric(tags.toList, _.mark(n))
}
