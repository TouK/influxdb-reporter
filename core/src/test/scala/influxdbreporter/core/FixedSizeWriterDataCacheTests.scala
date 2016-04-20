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

import org.scalatest.WordSpec

class FixedSizeWriterDataCacheTests extends WordSpec {

  "A FixedSizeWriterDataCache" should {

    "have max defined values of elements and remove old elements when is full and someone wants to add new ones" in {
      val cache = new FixedSizeWriterDataCache[String](4)

      assertResult(wd(1) :: wd(2) :: wd(3) :: Nil) {
        cache.update(add = wd(1) :: wd(2) :: wd(3) :: Nil)
        cache.get()
      }

      assertResult(wd(4) :: wd(5) :: wd(1) :: wd(2) :: Nil) {
        cache.update(add = wd(4) :: wd(5) :: Nil)
        cache.get()
      }

      assertResult(wd(4) :: wd(5) :: wd(2) :: Nil) {
        cache.update(remove = wd(1) :: Nil)
        cache.get()
      }

      assertResult(wd(6) :: wd(7) :: wd(4) :: wd(2) :: Nil) {
        cache.update(add = wd(6) :: wd(7) :: Nil, remove = wd(5) :: Nil)
        cache.get()
      }

      assertResult(wd(7) :: wd(4) :: Nil)  {
        cache.update(add = wd(7) :: Nil, remove = wd(6) :: wd(2) :: Nil)
        cache.get()
      }
    }
  }

  private def wd(num: Int) = new WriterData[String](num.toString)
}