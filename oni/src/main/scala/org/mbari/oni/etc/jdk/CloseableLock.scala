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

package org.mbari.oni.etc.jdk

import java.util.concurrent.locks.ReentrantLock

/**
 * An auto-closeable lock. Use in a try-with-resources block to ensure the lock is released.
 */
class CloseableLock extends ReentrantLock with AutoCloseable:

    def lockAndGet: CloseableLock =
        lock();
        this

    override def close(): Unit = unlock()
