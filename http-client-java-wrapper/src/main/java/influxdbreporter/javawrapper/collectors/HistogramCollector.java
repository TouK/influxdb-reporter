package influxdbreporter.javawrapper.collectors;

import com.codahale.metrics.Histogram;
import com.sun.istack.internal.NotNull;

public class HistogramCollector implements MetricCollector<Histogram> {

    public static final influxdbreporter.core.collectors.HistogramCollector COLLECTOR =
            influxdbreporter.core.collectors.HistogramCollector.apply();

    private final FieldMapperConverter converter;

    public HistogramCollector(@NotNull FieldMapper mapper) {
        if(mapper == null) throw new IllegalStateException("mapper cannot be null");
        this.converter = new FieldMapperConverter(mapper);
    }

    public influxdbreporter.core.collectors.HistogramCollector convertToScalaCollector() {
        return COLLECTOR.withFieldFlatMap(converter.mapperToScalaFunction1());
    }
}
