/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.sdk

/**
  * Brain-dead implementation of an IO monad. This is just a simple way to
  * represent a function that can fail. It's not a real IO monad, but it's
  * good enough for my purposes.
  * 
  * This doesn't model asyncrhonous operations, we're going to use sync 
  * operations with virual threads instead.
  */
type IO[A, B] = A => Either[Throwable, B]

object IO:
    extension [A, B](io: IO[A, B])

        def unit: IO[A, Unit] = a => Right(())

        def flatMap[C](f: IO[B, C]): IO[A, C] = a => io(a).flatMap(b => f(b)) 

        def map[C](f: B => C): IO[A, C] = a => io(a).map(f)

        def foreach(f: B => Unit): IO[A, Unit] = a =>
            for b <- io(a)
            yield f(b)
