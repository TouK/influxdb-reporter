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

import scala.collection.mutable.ListBuffer

trait MetricsRingBuffer[T] {

  def add(data: List[WriterData[T]]): List[WriterData[T]]

  def remove(data: List[WriterData[T]]): List[WriterData[T]]
}

class MetricsRingBufferImpl[T](maxSize: Int)
  extends MetricsRingBuffer[T] {

  private var ringBuffer: ListBuffer[WriterData[T]] = ListBuffer.empty

  override def add(data: List[WriterData[T]]): List[WriterData[T]] = synchronized {
    ringBuffer = (ringBuffer ++= data).take(maxSize)
    ringBuffer.toList
  }

  override def remove(data: List[WriterData[T]]): List[WriterData[T]] = synchronized {
    ringBuffer --= data
    ringBuffer.toList
  }
}