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
package influxdbreporter.core

trait Writer[T] {

  def write(measurement: String, fields: List[Field], tags: List[Tag], timestamp: Long): WriterData[T]

  def write(measurement: String, field: Field, tags: List[Tag], timestamp: Long): WriterData[T] =
    write(measurement, List(field), tags, timestamp)

  def write(measurement: String, field: Field, tag: Tag, timestamp: Long): WriterData[T] =
    write(measurement, List(field), List(tag), timestamp)

  def write(measurement: String, field: Field, timestamp: Long): WriterData[T] =
    write(measurement, List(field), List.empty, timestamp)
}

abstract class WriterData[T](val data: T) {
  def +(wd: WriterData[T]): WriterData[T]
}
