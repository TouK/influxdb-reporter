package influxdbreporter.javawrapper.collectors;

import com.codahale.metrics.Counter;
import com.sun.istack.internal.NotNull;

public class CounterCollector implements MetricCollector<Counter> {

    public static final influxdbreporter.core.collectors.CounterCollector COLLECTOR =
            influxdbreporter.core.collectors.CounterCollector.apply();

    private final FieldMapperConverter converter;

    public CounterCollector(@NotNull FieldMapper mapper) {
        if(mapper == null) throw new IllegalStateException("mapper cannot be null");
        this.converter = new FieldMapperConverter(mapper);
    }

    public influxdbreporter.core.collectors.CounterCollector convertToScalaCollector() {
        return COLLECTOR.withFieldMapper(converter.mapperToScalaFunction1());
    }
}
