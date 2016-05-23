package influxdbreporter.javawrapper.collectors;

import com.sun.istack.internal.Nullable;
import influxdbreporter.core.Field;

public interface FieldMapper {
    @Nullable Field map(Field field);
}
