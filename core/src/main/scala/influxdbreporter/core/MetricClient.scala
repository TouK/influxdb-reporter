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

import scala.concurrent.Future

trait MetricClient[T] {

  def sendData(data: List[WriterData[T]]): Future[Boolean]
  def stop(): Unit
}

trait MetricClientFactory[T] {
  def create(): MetricClient[T]
}

class SkipSendingClient extends MetricClient[String] {
  override def sendData(data: List[WriterData[String]]): Future[Boolean] = Future.successful(true)
  override def stop(): Unit = {}
}

object SkipSendingClientFactory extends MetricClientFactory[String] {
  override def create(): MetricClient[String] = new SkipSendingClient
}