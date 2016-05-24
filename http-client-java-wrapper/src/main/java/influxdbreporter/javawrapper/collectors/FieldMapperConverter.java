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
package influxdbreporter.javawrapper.collectors;

import influxdbreporter.core.Field;
import scala.Function1;
import scala.None$;
import scala.Option;
import scala.Some;
import scala.runtime.AbstractFunction1;

import javax.annotation.Nonnull;

class FieldMapperConverter {

    private final FieldMapper mapper;

    FieldMapperConverter(@Nonnull FieldMapper mapper) {
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