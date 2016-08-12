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

import influxdbreporter.core.writers.WriterData
import scala.language.postfixOps

trait Batcher[T] {

  def partition(data: List[WriterData[T]]): List[List[WriterData[T]]]
}

class DisabledBatching[T] extends Batcher[T] {
  override def partition(data: List[WriterData[T]]): List[List[WriterData[T]]] = List(data)
}

class SimpleBatcher[T](maxBatchSize: Int) extends Batcher[T] {

  override def partition(data: List[WriterData[T]]): List[List[WriterData[T]]] = {
    data grouped maxBatchSize toList
  }
}

object InfluxBatcher {
  val MaxBatchSize = 5000
}

class InfluxBatcher[T] extends SimpleBatcher[T](InfluxBatcher.MaxBatchSize)