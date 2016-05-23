package influxdbreporter.javawrapper.collectors;

import com.codahale.metrics.Meter;
import com.sun.istack.internal.NotNull;

public class MeterCollector implements MetricCollector<Meter> {

    public static final influxdbreporter.core.collectors.MeterCollector COLLECTOR =
            influxdbreporter.core.collectors.MeterCollector.apply();

    private final FieldMapperConverter converter;

    public MeterCollector(@NotNull FieldMapper mapper) {
        if(mapper == null) throw new IllegalStateException("mapper cannot be null");
        this.converter = new FieldMapperConverter(mapper);
    }

    public influxdbreporter.core.collectors.MeterCollector convertToScalaCollector() {
        return COLLECTOR.withFieldFlatMap(converter.mapperToScalaFunction1());
    }
}
