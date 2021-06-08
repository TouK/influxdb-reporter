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

import influxdbreporter.core.writers.{LineProtocolWriter, WriterData}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LineProtocolWriterTests extends AnyWordSpec with Matchers {

  private val lineProtocolWriter = new LineProtocolWriter()

  "A LineProtocolWriter" should {

    "generate proper Writer Data values" in {
      assertResult(WriterData[String]("measurement,t=2 f=1i 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", 1), Tag("t", 2), 1000000L)
      }

      assertResult(WriterData[String]("measurement,t=value2 f=\"value1\" 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", "value1"), Tag("t", "value2"), 1000000L)
      }

      assertResult(WriterData[String]("measurement,t=2 f=1.0 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", 1L), Tag("t", 2L), 1000000L)
      }

      assertResult(WriterData[String]("measurement,t=20.2 f=10.1 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", 10.10), Tag("t", 20.20), 1000000L)
      }

      assertResult(WriterData[String]("measurement,t=false f=true 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", true), Tag("t", false), 1000000L)
      }
    }

    "skip tag when value is empty" in {
      assertResult(WriterData[String]("measurement f=\"\" 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", ""), Tag("t", ""), 1000000L)
      }
    }

    "generate data with sorted tags" in {
      assertResult(WriterData[String]("measurement,a=2,b=2,c=2,d=2 f=1i 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", 1), Set(Tag("b", 2), Tag("d", 2), Tag("a", 2), Tag("c", 2)), 1000000L)
      }
    }

    "add static tags to returned result" in {
      assertResult(WriterData[String]("measurement,a=a1,b=b1,t=2 f=1i 1000000\n")) {
        new LineProtocolWriter(Tag("a", "a1") :: Tag("b", "b1") :: Nil)
          .write("measurement", Field("f", 1), Tag("t", 2), 1000000L)
      }
    }

    "escape given characters in measurement name" when {

      "char is ' '" in {
        assertResult(WriterData[String]("measurement\\ 1,a=2 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement 1", Field("f", 1), Tag("a", 2), 1000000L)
        }
      }

      "char is ','" in {
        assertResult(WriterData[String]("measurement\\,1,t=1 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement,1", Field("f", 1), Tag("t", 1), 1000000L)
        }
      }
    }

    "escape given characters in tag name" when {

      "char is '='" in {
        assertResult(WriterData[String]("measurement,a\\=c=2 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", 1), Tag("a=c", 2), 1000000L)
        }
      }

      "char is ' '" in {
        assertResult(WriterData[String]("measurement,tag\\ a=2 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", 1), Tag("tag a", 2), 1000000L)
        }
      }

      "char is ','" in {
        assertResult(WriterData[String]("measurement,t\\,a=1 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", 1), Tag("t,a", 1), 1000000L)
        }
      }
    }

    "escape given characters in tag value" when {

      "char is '='" in {
        assertResult(WriterData[String]("measurement,a=a\\=1 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", 1), Tag("a", "a=1"), 1000000L)
        }
      }

      "char is ' '" in {
        assertResult(WriterData[String]("measurement,a=val\\ 2 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", 1), Tag("a", "val 2"), 1000000L)
        }
      }

      "char is ','" in {
        assertResult(WriterData[String]("measurement,t=2\\,a f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", 1), Tag("t", "2,a"), 1000000L)
        }
      }
    }

    "escape given characters in field name" when {

      "char is '='" in {
        assertResult(WriterData[String]("measurement,a=1 f\\=a=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f=a", 1), Tag("a", 1), 1000000L)
        }
      }

      "char is ' '" in {
        assertResult(WriterData[String]("measurement,a=2 f\\ 1=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f 1", 1), Tag("a", 2), 1000000L)
        }
      }

      "char is ','" in {
        assertResult(WriterData[String]("measurement,t=1 f\\,a=1i 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f,a", 1), Tag("t", 1), 1000000L)
        }
      }
    }

    "escape given characters in field value" when {

      "char is '\"'" in {
        assertResult(WriterData[String]("measurement,t=1 f=\"a\\\"b\" 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", "a\"b"), Tag("t", 1), 1000000L)
        }
      }
    }

    "not escape" when {

      "'=' is in measurement name" in {
        assertResult(WriterData[String]("measurement=1,a=2 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement=1", Field("f", 1), Tag("a", 2), 1000000L)
        }
      }

      "'=' is in field value" in {
        assertResult(WriterData[String]("measurement,a=1 f=\"val 1\" 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", "val 1"), Tag("a", 1), 1000000L)
        }
      }

      "space is is field value" in {
        assertResult(WriterData[String]("measurement,a=2 f=\"1 a\" 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", "1 a"), Tag("a", 2), 1000000L)
        }
      }

      "',' is in field value" in {
        assertResult(WriterData[String]("measurement,a=2 f=\"1,a\" 1000000\n")) {
          lineProtocolWriter.write("measurement", Field("f", "1,a"), Tag("a", 2), 1000000L)
        }
      }

      "'\"' in measurement name" in {
        assertResult(WriterData[String]("measurement\"1,a=2 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement\"1", Field("f", 1), Tag("a", 2), 1000000L)
        }
      }

      "'\"' in tag name" in {
        assertResult(WriterData[String]("measurement1,a\"1=2 f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement1", Field("f", 1), Tag("a\"1", 2), 1000000L)
        }
      }

      "'\"' in tag value" in {
        assertResult(WriterData[String]("measurement1,a=2\"a f=1i 1000000\n")) {
          lineProtocolWriter.write("measurement1", Field("f", 1), Tag("a", "2\"a"), 1000000L)
        }
      }

      "'\"' in field name" in {
        assertResult(WriterData[String]("measurement1,a=2 f\"a=1i 1000000\n")) {
          lineProtocolWriter.write("measurement1", Field("f\"a", 1), Tag("a", 2), 1000000L)
        }
      }
    }

    "distinct tags by name" in {
      assertResult(WriterData[String]("measurement,t=2 f=1i 1000000\n")) {
        lineProtocolWriter.write("measurement", Field("f", 1), Set(Tag("t", 2), Tag("t", 2)), 1000000L)
      }

      lineProtocolWriter.write("measurement", Field("f", 1), Set(Tag("t", 2), Tag("t", 1)), 1000000L)
        .data should fullyMatch regex "measurement,t=\\d f=1i 1000000\n"

      val lineProtocolWriterWithOneStaticTag = new LineProtocolWriter(Tag("t", 2) :: Nil)
      lineProtocolWriterWithOneStaticTag.write("measurement", Field("f", 1), Set(Tag("t", 2)), 1000000L)
        .data should fullyMatch regex "measurement,t=\\d f=1i 1000000\n"

      val lineProtocolWriterWithTwoStaticTags = new LineProtocolWriter(Tag("t", 2) :: Tag("t", 1) :: Nil)
      lineProtocolWriterWithTwoStaticTags.write("measurement", Field("f", 1), Set(Tag("t", 2)), 1000000L)
        .data should fullyMatch regex "measurement,t=\\d f=1i 1000000\n"

    }
  }
}