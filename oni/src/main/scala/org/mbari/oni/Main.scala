/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni

import io.helidon.webserver.WebServer
import org.mbari.oni.etc.jdk.Loggers
import org.mbari.oni.etc.jdk.Loggers.given
import sttp.tapir.server.nima.{NimaServerInterpreter, NimaServerOptions}

object Main:

    def main(args: Array[String]): Unit =

        System.setProperty("user.timezone", "UTC")

        val s =
            """
        |_______/\\\\\__________________________
        | _____/\\\///\\\________________________
        |  ___/\\\/__\///\\\_________________/\\\_
        |   __/\\\______\//\\\__/\\/\\\\\\___\///__
        |    _\/\\\_______\/\\\_\/\\\////\\\___/\\\_
        |     _\//\\\______/\\\__\/\\\__\//\\\_\/\\\_
        |      __\///\\\__/\\\____\/\\\___\/\\\_\/\\\_
        |       ____\///\\\\\/_____\/\\\___\/\\\_\/\\\_
        |        ______\/////_______\///____\///__\///__""".stripMargin + s"  v${AppConfig.Version}"
        println(s)

        val log  = System.getLogger(getClass.getName)
        val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
        log.atInfo.log(s"Starting ${AppConfig.Name} v${AppConfig.Version} on port $port")

        val serverOptions = NimaServerOptions
            .customiseInterceptors
            .serverLog(None)
            .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
            .options

        val handler = NimaServerInterpreter(serverOptions).toHandler(Endpoints.allImpl)

        WebServer
            .builder()
            .routing(builder => builder.any(handler))
            .port(port)
            .build()
            .start()
