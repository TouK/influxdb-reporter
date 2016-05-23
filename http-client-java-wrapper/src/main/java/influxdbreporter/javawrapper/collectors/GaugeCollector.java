package influxdbreporter.javawrapper.collectors;

import com.codahale.metrics.Gauge;
import com.sun.istack.internal.NotNull;

public class GaugeCollector<T> implements MetricCollector<Gauge<T>> {

    public static <T> influxdbreporter.core.collectors.GaugeCollector<T> collector() {
        return influxdbreporter.core.collectors.GaugeCollector.apply();
    }

    private final FieldMapperConverter converter;

    public GaugeCollector(@NotNull FieldMapper mapper) {
        if (mapper == null) throw new IllegalStateException("mapper cannot be null");
        this.converter = new FieldMapperConverter(mapper);
    }

    public influxdbreporter.core.collectors.GaugeCollector<T> convertToScalaCollector() {
        return influxdbreporter.core.collectors.GaugeCollector
                .<T>apply()
                .withFieldMapper(converter.mapperToScalaFunction1());
    }
}
