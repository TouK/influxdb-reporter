package influxdbreporter.core.metrics.pull

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import com.codahale.metrics.Clock

abstract class PullingCachedGauge[V] protected (clock: Clock,
                                                timeout: Long,
                                                timeoutUnit: TimeUnit) extends PullingGauge[V] {

  private val reloadAt = new AtomicLong(0)

  private val timeoutNS = timeoutUnit.toNanos(timeout)

  @volatile private var value: List[ValueByTag[V]] = _

  protected def this(timeout: Long, timeoutUnit: TimeUnit) {
    this(Clock.defaultClock(), timeout, timeoutUnit)
  }

  protected def loadValue(): List[ValueByTag[V]]

  override def getValues: List[ValueByTag[V]] = {
    if (shouldLoad()) {
      this.value = loadValue()
    }
    value
  }

  private def shouldLoad(): Boolean = {
    val time = clock.getTick
    val current = reloadAt.get
    current <= time && reloadAt.compareAndSet(current, time + timeoutNS)
  }

}