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

import influxdbreporter.core.writers.{Writer, WriterData}
import org.scalatest.WordSpec
import org.scalatest.concurrent.Waiters.Waiter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

abstract class BaseMetricTest extends WordSpec with ScalaFutures {

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global

  protected def createMockWriter(phaseChange: PhaseChangeAction,
                                 assertPhase: PhaseAssert) =
    new MockInfluxdbWriterWithPhaseAssertions(phaseChange, assertPhase)

  protected def createMockMetricClientFactory(writer: MockInfluxdbWriterWithPhaseAssertions) =
    new MetricClientFactory[List[TestData]] {
      override def create(): MetricClient[List[TestData]] = new MockMetricClient(writer)
    }

  sealed trait Phase

  case object PhaseOne extends Phase

  case object PhaseTwo extends Phase

  case object PhaseThree extends Phase

  type PhaseChangeAction = Phase => Unit
  type PhaseAssert = (Phase, String, List[Field], Set[Tag]) => Boolean

  class MockMetricClient(writer: MockInfluxdbWriterWithPhaseAssertions) extends MetricClient[List[TestData]] {
    override def sendData(data: List[WriterData[List[TestData]]]): Future[Boolean] = {
      writer.nextPhase()
      Future.successful(true)
    }
    override def stop(): Unit = {}
  }

  class MockInfluxdbWriterWithPhaseAssertions(phaseChange: PhaseChangeAction,
                                              assertPhase: PhaseAssert)
    extends Writer[List[TestData]] {

    private val waiter = new Waiter
    private var currentPhase: Phase = PhaseOne
    phaseChange(safeCurrentPhase())

    override def write(measurement: String, fields: List[Field], tags: Set[Tag], timestamp: Long): WriterData[List[TestData]] = {
      val phase = safeCurrentPhase()
      if (!assertPhase(phase, measurement, fields, tags)) {
        waiter(fail(s"In phase $phase these data {[name:[$measurement], fields:[$fields], tags:[$tags]} were not expected"))
      }
      WriterData(TestData(measurement, fields, tags) :: Nil)
    }

    def nextPhase(): Unit = phaseChange {
      safeNextPhase()
    }

    def waitToPhaseThreeEnds(): Unit = {
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

  case class TestData(name: String, fields: List[Field], tags: Set[Tag])

}
