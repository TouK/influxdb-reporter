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

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer

trait WriterDataBuffer[T] {

  def update(add: List[WriterData[T]] = Nil, remove: List[WriterData[T]] = Nil): List[WriterData[T]]

  def get(): List[WriterData[T]]
}

class FixedSizeWriterDataBuffer[T](maxSize: Int)
  extends WriterDataBuffer[T] {

  private var ringBuffer: ListBuffer[WriterData[T]] = ListBuffer.empty

  override def update(add: List[WriterData[T]] = Nil, remove: List[WriterData[T]] = Nil): List[WriterData[T]] = {
    if (add.nonEmpty || remove.nonEmpty) synchronized {
      ringBuffer --= remove
      add ++=: ringBuffer
      ringBuffer = ringBuffer.distinct.take(maxSize)
    }
    ringBuffer.toList
  }

  override def get(): List[WriterData[T]] = ringBuffer.toList

}