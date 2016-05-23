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

import influxdbreporter.core.collectors.{CollectorOps, MetricCollector}
import influxdbreporter.core.metrics.Metric._
import influxdbreporter.core.metrics._
import influxdbreporter.core.metrics.pull.PullingCodehaleMetric
import influxdbreporter.core.metrics.push._

import scala.collection.concurrent.TrieMap

trait MetricRegistry {

  def register[T](metricName: String, magnet: RegisterMagnet[T]): T

  def unregister(name: String): Unit

  def getMetricsMap: Map[String, (Metric[CodahaleMetric], MetricCollector[CodahaleMetric])]

}

object MetricRegistry {
  def apply(prefix: String): MetricRegistry = new MetricRegistryImpl(prefix)
}

private class MetricRegistryImpl(prefix: String) extends MetricRegistry {

  private val metrics = TrieMap[String, (Metric[CodahaleMetric], MetricCollector[CodahaleMetric])]()

  def getMetricsMap: Map[String, (Metric[CodahaleMetric], MetricCollector[CodahaleMetric])] = metrics.toMap

  override def register[T](metricName: String, magnet: RegisterMagnet[T]): T = magnet(metricName, this)

  def registerMetricWithCollector[T <: Metric[U], U <: CodahaleMetric](name: String, metric: T, collector: MetricCollector[U]): T = {
    val metricName = createMetricName(name)
    val previouslyRegisteredMetric = metrics.putIfAbsent(
      metricName,
      (metric.asInstanceOf[Metric[CodahaleMetric]], collector.asInstanceOf[MetricCollector[CodahaleMetric]])
    )
    if (previouslyRegisteredMetric.isDefined)
      throw new IllegalArgumentException(s"Can't register metric. There is already defined metric for $metricName")
    else
      metric
  }

  override def unregister(name: String): Unit = {
    metrics.remove(createMetricName(name))
  }

  private def createMetricName(name: String) = s"$prefix.$name"

}

trait RegisterMagnet[T] {

  def apply(metricName: String, registryImpl: MetricRegistryImpl): T
}

object RegisterMagnet {

  private val registerMagnetForCounters = new RegisterMagnetFromMetric[Counter, CodahaleCounter]
  private val registerMagnetForCodehaleCounters = new RegisterMagnetFromCodehaleMetric[CodahaleCounter]
  private val registerMagnetForHistogram = new RegisterMagnetFromMetric[Histogram, CodahaleHistogram]
  private val registerMagnetForCodehaleHistogram = new RegisterMagnetFromCodehaleMetric[CodahaleHistogram]
  private val registerMagnetForMeter = new RegisterMagnetFromMetric[Meter, CodahaleMeter]
  private val registerMagnetForCodehaleMeter = new RegisterMagnetFromCodehaleMetric[CodahaleMeter]
  private val registerMagnetForTimer = new RegisterMagnetFromMetric[Timer, CodahaleTimer]
  private val registerMagnetForCodehaleTimer = new RegisterMagnetFromCodehaleMetric[CodahaleTimer]
  private def registerMagnetForGauge[T] = new RegisterMagnetFromMetric[DiscreteGauge[T], CodahaleGauge[T]]()

  implicit def influxCounterToRegisterMagnet(counter: Counter): RegisterMagnet[Counter] = {
    registerMagnetForCounters(counter)
  }

  implicit def influxCounterWithCollectorToRegisterMagnet(counterAndCollector: (Counter, MetricCollector[CodahaleCounter])):
  RegisterMagnet[Counter] = {
    val (counter, collector) = counterAndCollector
    registerMagnetForCounters(counter, Some(collector))
  }

  implicit def counterToRegisterMagnet(counter: CodahaleCounter): RegisterMagnet[CodahaleCounter] = {
    registerMagnetForCodehaleCounters(counter)
  }

  implicit def counterWithCollectorToRegisterMagnet(counterAndCollector: (CodahaleCounter, MetricCollector[CodahaleCounter])):
  RegisterMagnet[CodahaleCounter] = {
    val (counter, collector) = counterAndCollector
    registerMagnetForCodehaleCounters(counter, Some(collector))
  }

  implicit def pullingGaugeToRegisterMagnet[T](gauge: Metric[CodahaleGauge[T]]): RegisterMagnet[Metric[CodahaleGauge[T]]] = {
    (new RegisterMagnetFromMetric[Metric[CodahaleGauge[T]], CodahaleGauge[T]]()(CollectorOps.CollectorForGauge[T]))(gauge)
  }

  implicit def discreteGaugeToRegisterMagnet[T](gauge: DiscreteGauge[T]): RegisterMagnet[DiscreteGauge[T]] = {
    registerMagnetForGauge[T].apply(gauge)
  }

  implicit def discreteGaugeWithCollectorToRegisterMagnet[T](gaugeAndCollector: (DiscreteGauge[T], MetricCollector[CodahaleGauge[T]])):
  RegisterMagnet[DiscreteGauge[T]] = {
    val (gauge, collector) = gaugeAndCollector
    registerMagnetForGauge[T].apply(gauge, Some(collector))
  }

  implicit def gaugeToRegisterMagnet[T](gauge: CodahaleGauge[T]): RegisterMagnet[CodahaleGauge[T]] = {
    (new RegisterMagnetFromCodehaleMetric[CodahaleGauge[T]]()(CollectorOps.CollectorForGauge[T]))(gauge)
  }

  implicit def gaugeWithCollectorToRegisterMagnet[T](gaugeAndCollector: (CodahaleGauge[T], MetricCollector[CodahaleGauge[T]])):
  RegisterMagnet[CodahaleGauge[T]] = {
    val (gauge, collector) = gaugeAndCollector
    (new RegisterMagnetFromCodehaleMetric[CodahaleGauge[T]]()(CollectorOps.CollectorForGauge[T]))(gauge, Some(collector))
  }

  implicit def influxHistogramToRegisterMagnet(histogram: Histogram): RegisterMagnet[Histogram] = {
    registerMagnetForHistogram(histogram)
  }

  implicit def influxHistogramWithCollectorToRegisterMagnet(nameAndHistogramAndCollector: (Histogram, MetricCollector[CodahaleHistogram])):
  RegisterMagnet[Histogram] = {
    val (histogram, collector) = nameAndHistogramAndCollector
    registerMagnetForHistogram(histogram, Some(collector))
  }

  implicit def histogramToRegisterMagnet(histogram: CodahaleHistogram): RegisterMagnet[CodahaleHistogram] = {
    registerMagnetForCodehaleHistogram(histogram)
  }

  implicit def histogramWithCollectorToRegisterMagnet(histogramAndCollector: (CodahaleHistogram, MetricCollector[CodahaleHistogram])):
  RegisterMagnet[CodahaleHistogram] = {
    val (histogram, collector) = histogramAndCollector
    registerMagnetForCodehaleHistogram(histogram, Some(collector))
  }

  implicit def influxMeterToRegisterMagnet(meter: Meter): RegisterMagnet[Meter] = {
    registerMagnetForMeter(meter)
  }

  implicit def influxMeterWithCollectorToRegisterMagnet(meterAndCollector: (Meter, MetricCollector[CodahaleMeter])):
  RegisterMagnet[Meter] = {
    val (meter, collector) = meterAndCollector
    registerMagnetForMeter(meter, Some(collector))
  }

  implicit def meterToRegisterMagnet(meter: CodahaleMeter): RegisterMagnet[CodahaleMeter] = {
    registerMagnetForCodehaleMeter(meter)
  }

  implicit def meterWithCollectorToRegisterMagnet(meterAndCollector: (CodahaleMeter, MetricCollector[CodahaleMeter])):
  RegisterMagnet[CodahaleMeter] = {
    val (meter, collector) = meterAndCollector
    registerMagnetForCodehaleMeter(meter, Some(collector))
  }

  implicit def influxTimerToRegisterMagnet(timer: Timer): RegisterMagnet[Timer] = {
    registerMagnetForTimer(timer)
  }

  implicit def influxTimerWithCollectorToRegisterMagnet(timerAndCollector: (Timer, MetricCollector[CodahaleTimer])):
  RegisterMagnet[Timer] = {
    val (timer, collector) = timerAndCollector
    registerMagnetForTimer(timer, Some(collector))
  }

  implicit def timerToRegisterMagnet(timer: CodahaleTimer): RegisterMagnet[CodahaleTimer] = {
    registerMagnetForCodehaleTimer(timer)
  }

  implicit def timerWithCollectorToRegisterMagnet(timerAndCollector: (CodahaleTimer, MetricCollector[CodahaleTimer])):
  RegisterMagnet[CodahaleTimer] = {
    val (timer, collector) = timerAndCollector
    registerMagnetForCodehaleTimer(timer, Some(collector))
  }

  private class RegisterMagnetFromMetric[U <: Metric[T], T <: CodahaleMetric](implicit co: CollectorOps[T]) {

    def apply(metric: U, collector: Option[MetricCollector[T]] = None): RegisterMagnet[U] =
      new RegisterMagnet[U] {
        override def apply(metricName: String, registryImpl: MetricRegistryImpl): U =
          registryImpl.registerMetricWithCollector(metricName, metric, collector.getOrElse(co.collector))
      }
  }

  private class RegisterMagnetFromCodehaleMetric[T <: CodahaleMetric](implicit co: CollectorOps[T]) {

    def apply(metric: T, collector: Option[MetricCollector[T]] = None): RegisterMagnet[T] =
      new RegisterMagnet[T] {
        override def apply(metricName: String, registryImpl: MetricRegistryImpl): T = {
          registryImpl.registerMetricWithCollector(metricName, new PullingCodehaleMetric(metric), collector.getOrElse(co.collector))
          metric
        }
      }
  }

}