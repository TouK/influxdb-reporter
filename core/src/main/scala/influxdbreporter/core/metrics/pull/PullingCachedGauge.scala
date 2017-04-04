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
package influxdbreporter.core.metrics.pull

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import influxdbreporter.core.utils.ClockOpt.toClockOpt

import com.codahale.metrics.Clock

import scala.concurrent.{ExecutionContext, Future}

abstract class PullingCachedGauge[V] protected (clock: Clock,
                                                timeout: Long,
                                                timeoutUnit: TimeUnit) extends PullingGauge[V] {

  private val reloadAt = new AtomicLong(0)
  private val timeoutNS = timeoutUnit.toNanos(timeout)

  @volatile private var valueFuture: Future[List[ValueByTag[V]]] = _

  protected def this(timeout: Long, timeoutUnit: TimeUnit) {
    this(Clock.defaultClock(), timeout, timeoutUnit)
  }

  protected def loadValue()(implicit ec: ExecutionContext): Future[List[ValueByTag[V]]]

  override def getValues(implicit ec: ExecutionContext): Future[List[ValueByTag[V]]] = {
    if (shouldLoad()) {
      valueFuture = loadValue()
    }
    valueFuture
  }

  private def shouldLoad(): Boolean = {
    val time = clock.getTimeInNanos
    val current = reloadAt.get
    time > current && reloadAt.compareAndSet(current, time + timeoutNS)
  }

}