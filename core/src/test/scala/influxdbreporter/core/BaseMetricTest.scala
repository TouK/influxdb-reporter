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
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseMetricTest extends WordSpec with ScalaFutures {

  implicit val ex = ExecutionContext.global

  protected def createMockWriter(phaseChange: PhaseChangeAction,
                                 assertPhase: PhaseAssert) =
    new MockInfluxdbWriterWithPhaseAssertions(phaseChange, assertPhase)

  protected def createMockMetricClient(writer: MockInfluxdbWriterWithPhaseAssertions) =
    new MockMetricClient(writer)

  sealed trait Phase

  case object PhaseOne extends Phase

  case object PhaseTwo extends Phase

  case object PhaseThree extends Phase

  type PhaseChangeAction = Phase => Unit
  type PhaseAssert = (Phase, String, List[Field], List[Tag]) => Boolean

  class MockMetricClient(writer: MockInfluxdbWriterWithPhaseAssertions) extends MetricClient[List[TestData]] {
    override def sendData(data: WriterData[List[TestData]]): Future[Boolean] = {
      writer.nextPhase()
      Future.successful(true)
    }
  }

  class MockInfluxdbWriterWithPhaseAssertions(phaseChange: PhaseChangeAction,
                                              assertPhase: PhaseAssert)
    extends Writer[List[TestData]] {

    private val waiter = new Waiter
    private var currentPhase: Phase = PhaseOne
    phaseChange(safeCurrentPhase())

    override def write(measurement: String, fields: List[Field], tags: List[Tag], timestamp: Long): WriterData[List[TestData]] = {
      val phase = safeCurrentPhase()
      if (!assertPhase(phase, measurement, fields, tags)) {
        waiter(fail(s"In phase $phase these data {[name:[$measurement], fields:[$fields], tags:[$tags]} were not expected"))
      }
      TestWriterData(TestData(measurement, fields, tags) :: Nil)
    }

    def nextPhase() = phaseChange {
      safeNextPhase()
    }

    def waitToPhaseThreeEnds() = {
      val testTimeout = org.scalatest.concurrent.PatienceConfiguration.Timeout(Span(10, Seconds))
      waiter.await(testTimeout)
    }

    private def safeCurrentPhase() = synchronized(currentPhase)

    private def safeNextPhase() = synchronized {
      val newPhase = currentPhase match {
        case PhaseOne => PhaseTwo
        case PhaseTwo => PhaseThree
        case PhaseThree =>
          waiter.dismiss()
          PhaseThree
      }
      currentPhase = newPhase
      newPhase
    }

  }

  case class TestData(name: String, fields: List[Field], tags: List[Tag])

  case class TestWriterData(testData: List[TestData]) extends WriterData(testData) {
    override def +(wd: WriterData[List[TestData]]): WriterData[List[TestData]] =
      TestWriterData(wd.data ::: testData)
  }

}
