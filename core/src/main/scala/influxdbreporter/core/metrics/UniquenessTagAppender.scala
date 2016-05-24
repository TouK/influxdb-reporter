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
import influxdbreporter.core.metrics.Metric.CodahaleMetric
import influxdbreporter.core.metrics.MetricByTag.MetricByTags

// read more:
// https://github.com/influxdata/docs.influxdata.com/blob/master/content/influxdb/v0.9/concepts/08_vs_09.md#points-with-identical-timestamps
protected trait UniquenessTagAppender {

  protected def mapListByAddingUniqueTagToEachMetric[U <: CodahaleMetric](metricByTags: MetricByTags[U]): MetricByTags[U] = {
    metricByTags.zipWithIndex.map {
      case (metricByTag, idx) =>
        metricByTag.copy[U](tags = Tag("u", idx) :: metricByTag.tags)
    }
  }
}