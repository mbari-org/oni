/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jdk

import java.lang.System.Logger
import java.lang.System.Logger.Level
import java.util.function.Supplier

/**
 * Add fluent logging to System.Logger. Usage:
 * 
 * ```scala
 * import org.fathomnet.support.etc.jdk.Loggers.{given, *}
 * given log: Logger = System.getLogger("my.logger")
 *
 * log.atInfo.log("Hello World")
 * log.atInfo.withCause(new RuntimeException("Oops")).log("Hello World")
 *
 * 3.tapLog.atInfo.log(i => s"Hello World $i")
 * ```
 * * @author Brian Schlining
 */
object Loggers:

    trait Builder:
        def logger: Logger
        def level: Level
        def throwable: Option[Throwable]

    case class LoggerBuilder(
        logger: Logger,
        level: Level = Level.OFF,
        throwable: Option[Throwable] = None
    ):

        def atTrace: LoggerBuilder = copy(level = Level.TRACE)
        def atDebug: LoggerBuilder = copy(level = Level.DEBUG)
        def atInfo: LoggerBuilder  = copy(level = Level.INFO)
        def atWarn: LoggerBuilder  = copy(level = Level.WARNING)
        def atError: LoggerBuilder = copy(level = Level.ERROR)

        def withCause(cause: Throwable): LoggerBuilder = copy(throwable = Some(cause))

        def log(msg: => String): Unit =
            if logger.isLoggable(level) then
                throwable match
                    case Some(e) => logger.log(level, msg, e)
                    case None    => logger.log(level, msg)

        def log(fn: Supplier[String]): Unit =
            if logger.isLoggable(level) then
                throwable match
                    case Some(e) => logger.log(level, fn, e)
                    case None    => logger.log(level, fn)

    given Conversion[Logger, LoggerBuilder] with
        def apply(logger: Logger): LoggerBuilder = LoggerBuilder(logger)

    def apply(name: String): Logger = System.getLogger(name)

    def apply(clazz: Class[?]): Logger = System.getLogger(clazz.getName)
