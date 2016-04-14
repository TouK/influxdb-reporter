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

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures

class RingBufferTest extends WordSpec with ScalaFutures with Matchers {

  "A test" in {
    val ringbuffer = new MetricsRingBufferImpl[String](2)
    val wd1 = WriterData("1")
    val wd2 = WriterData("2")
    val wd3 = WriterData("3")
    ringbuffer.add(wd1 :: wd2 :: wd3 :: Nil)
    ringbuffer.remove(wd2 :: Nil) should be (wd1 :: Nil)
  }
}