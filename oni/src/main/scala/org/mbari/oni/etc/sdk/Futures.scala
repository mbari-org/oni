/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
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

package org.mbari.oni.etc.sdk

import java.time.Duration as JDuration
import scala.concurrent.duration.Duration as SDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.DurationConverters.*
import scala.util.Try

object Futures:
    val DefaultTimeout = JDuration.ofSeconds(10)

    /**
     * Run a Future and return the result or an Exception if the Future fails or does not complete within the timeout
     *
     * @param f
     *   A function that returns a Future
     * @param timeout
     *   The maximum amount of time to wait for the Future to complete
     * @tparam T
     *   The type that the Future returns
     * @return
     *   The result of the Future or an Exception
     */
    def safeRunSync[T](f: => Future[T], timeout: JDuration)(using ec: ExecutionContext): Either[Throwable, T] =
        Try(Await.result(f, timeout.toScala)).toEither

    extension [T](f: Future[T])
        def join: T                                                                                            = join(DefaultTimeout)
        def join(duration: JDuration): T                                                                       = join(duration.toScala)
        def join(duration: SDuration): T                                                                       = Await.result(f, duration)
        def safeRunSync(timeout: JDuration = DefaultTimeout)(using ec: ExecutionContext): Either[Throwable, T] =
            Futures.safeRunSync(f, timeout)
