/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jdk

import java.util.concurrent.locks.ReentrantLock

class CloseableLock extends ReentrantLock with AutoCloseable {

    def lockAndGet: CloseableLock = {
        lock();
        this
    }

    override def close(): Unit = unlock()

}
