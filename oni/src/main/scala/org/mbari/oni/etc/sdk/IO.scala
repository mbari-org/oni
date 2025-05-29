/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.sdk

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A simplified IO monad, representing a function that can fail. Designed for simplicity, not full IO monad features.
 * Uses synchronous operations, intended for use with virtual threads.
 */
type IO[A, B]      = A => Either[Throwable, B]
type AsyncIO[A, B] = A => Future[B]

object IO:

    /**
     * Creates an IO that always succeeds with Unit.
     */
    def unit[A]: IO[A, Unit] = _ => Right(())

    /**
     * Creates an IO that always succeeds with the given value.
     */
    def pure[B](b: B): IO[Any, B] = _ => Right(b)

    extension [A, B](io: IO[A, B])

        /**
         * Chains this IO with another, executing the second only if the first succeeds.
         */
        def flatMap[C](f: IO[B, C]): IO[A, C] = a =>
            io(a) match
                case Right(b) => f(b)
                case Left(e)  => Left(e)

        /**
         * Transforms the successful result of this IO.
         */
        def map[C](f: B => C): IO[A, C] = a => io(a).map(f)

        /**
         * Performs a side effect with the successful result of this IO.
         */
        def foreach(f: B => Unit): IO[A, Unit] = a =>
            io(a) match
                case Right(b) =>
                    f(b)
                    Right(()) // Ensure we return a Right(())
                case Left(e) => Left(e)

        /**
         * Converts this IO to an AsyncIO (a function returning a Future).
         */
        def async(using executionContext: ExecutionContext): AsyncIO[A, B] = a =>
            Future { // Use Future { ... } for more concise async
                io(a) match
                    case Right(b) => b
                    case Left(e)  => throw e // Use throw e to reject the Future
            }

        /**
         * Runs the IO, producing the Either result.
         */
        def run(a: A): Either[Throwable, B] = io(a)

        /**
         * Handles the errors, and returns an IO
         */
        def handleError(handler: Throwable => B): IO[A, B] = a =>
            io(a) match
                case Left(e) => Right(handler(e))
                case r       => r

        /**
         * Handles the errors, and returns an IO
         */
        def handleErrorWith(handler: Throwable => IO[A, B]): IO[A, B] = a =>
            io(a) match
                case Left(e) => handler(e)(a)
                case r       => r
