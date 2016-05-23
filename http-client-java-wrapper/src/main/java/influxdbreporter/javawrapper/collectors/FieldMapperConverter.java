package influxdbreporter.javawrapper.collectors;

import com.sun.istack.internal.NotNull;
import influxdbreporter.core.Field;
import scala.Function1;
import scala.None$;
import scala.Option;
import scala.Some;
import scala.runtime.AbstractFunction1;

class FieldMapperConverter {

    private final FieldMapper mapper;

    FieldMapperConverter(@NotNull FieldMapper mapper) {
        if(mapper == null) throw new IllegalStateException("mapper cannot be null");
        this.mapper = mapper;
    }

    Function1<Field, Option<Field>> mapperToScalaFunction1() {
        return new AbstractFunction1<Field, Option<Field>>() {
            @Override
            public Option<Field> apply(Field field) {
                Field mappedField = mapper.map(field);
                return mappedField != null ? new Some<>(mappedField) : None$.<Field>empty();
            }
        };
    }
}
