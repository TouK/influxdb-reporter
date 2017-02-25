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
package influxdbreporter

import java.io.Closeable

import org.asynchttpclient.{AsyncCompletionHandler, AsyncHttpClient, Request, Response}

import scala.concurrent.{Future, Promise}

class AsyncHttpClientWrapper(val underlying: AsyncHttpClient with Closeable) {

  def send(request: Request): Future[Response] = {
    val result = Promise[Response]()
    underlying.executeRequest(request, new AsyncCompletionHandler[Response]() {
      override def onCompleted(response: Response): Response = {
        result.success(response)
        response
      }

      override def onThrowable(t: Throwable): Unit = {
        result.failure(t)
      }
    })
    result.future
  }

}