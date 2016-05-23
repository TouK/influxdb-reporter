package influxdbreporter.javawrapper.collectors;

import com.codahale.metrics.Timer;
import com.sun.istack.internal.NotNull;
import influxdbreporter.core.collectors.SecondTimerCollector$;

public class TimerCollector implements MetricCollector<Timer> {

    public static final influxdbreporter.core.collectors.TimerCollector COLLECTOR =
            SecondTimerCollector$.MODULE$;

    private final FieldMapperConverter converter;

    public TimerCollector(@NotNull FieldMapper mapper) {
        if (mapper == null) throw new IllegalStateException("mapper cannot be null");
        this.converter = new FieldMapperConverter(mapper);
    }

    public influxdbreporter.core.collectors.TimerCollector convertToScalaCollector() {
        return COLLECTOR.withFieldMapper(converter.mapperToScalaFunction1());
    }
}