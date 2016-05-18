package influxdbreporter.core

import org.scalatest.WordSpec

class LineProtocolWriterTests extends WordSpec {

  "A LineProtocolWriter" should {

    "generate proper Writer Data values" in {
      assertResult(WriterData[String]("measurement,t=2 f=1 1000000\n")) {
        LineProtocolWriter.write("measurement", Field("f", 1), Tag("t", 2), 1000000L)
      }

      assertResult(WriterData[String]("measurement,t=\"value2\" f=\"value1\" 1000000\n")) {
        LineProtocolWriter.write("measurement", Field("f", "value1"), Tag("t", "value2"), 1000000L)
      }

      assertResult(WriterData[String]("measurement,t=20.2 f=10.1 1000000\n")) {
        LineProtocolWriter.write("measurement", Field("f", 10.10), Tag("t", 20.20), 1000000L)
      }

      assertResult(WriterData[String]("measurement,t=false f=true 1000000\n")) {
        LineProtocolWriter.write("measurement", Field("f", true), Tag("t", false), 1000000L)
      }
    }

    "properly escape space character" in {
      assertResult(WriterData[String]("measurement\\ 1,t\\ 2=\"tv\\ 2\" f\\ 1=\"fv\\ 1\" 1000000\n")) {
        LineProtocolWriter.write("measurement 1", Field("f 1", "fv 1"), Tag("t 2", "tv 2"), 1000000L)
      }
    }

    "properly escape comma character" in {
      assertResult(WriterData[String]("measurement\\,1,t\\,2=\"tv\\,2\" f\\,1=\"fv\\,1\" 1000000\n")) {
        LineProtocolWriter.write("measurement,1", Field("f,1", "fv,1"), Tag("t,2", "tv,2"), 1000000L)
      }
    }
  }
}
