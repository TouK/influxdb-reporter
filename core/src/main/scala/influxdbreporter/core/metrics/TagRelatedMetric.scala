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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

import Metric.CodehaleMetric
import MetricByTag.{InfluxdbTags, MetricByTags}

import scala.collection.JavaConverters._

// T must be thread safe
private[metrics] abstract class TagRelatedMetric[T <: CodehaleMetric] {

  type MetricAction[M <: CodehaleMetric] = M => Unit

  private val metricByTags = new AtomicReference(new ConcurrentHashMap[OrderIndependentList, T])

  protected def createMetric(): T

  protected def increaseMetric(tags: InfluxdbTags, action: MetricAction[T]): Unit = {
    lazy val newMetric = createMetric()
    Option(metricByTags.get().putIfAbsent(OrderIndependentList(tags), newMetric)) match {
      case Some(oldMetric) =>
        action(oldMetric)
      case None =>
        action(newMetric)
    }
  }

  def popMetrics: MetricByTags[T] = {
    val snapshot = metricByTags.getAndSet(new ConcurrentHashMap[OrderIndependentList, T]())
    snapshot.asScala.toList.map {
      case (tagsWrapper, metric) => MetricByTag(tagsWrapper.tags, metric)
    }
  }

  private case class OrderIndependentList(tags: InfluxdbTags) {
    override def hashCode(): Int = {
      val prime = 17
      val result = tags.map(_.hashCode()).sorted.foldLeft(1){
        case (acc, hash) => acc * prime + hash
      }
      result
    }

    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case that: OrderIndependentList =>
          this.tags.diff(that.tags).isEmpty
        case _ =>
          false
      }
    }
  }

}
