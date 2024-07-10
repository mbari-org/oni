/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni

import org.mbari.oni.etc.jdk.Loggers
import org.mbari.oni.etc.jdk.Loggers.given
import sttp.tapir.server.vertx.VertxFutureServerOptions
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import sttp.tapir.server.vertx.{VertxFutureServerInterpreter, VertxFutureServerOptions}
import sttp.tapir.server.vertx.VertxFutureServerInterpreter.VertxFutureToScalaFuture
import io.vertx.core.VertxOptions

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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

        val serverOptions = VertxFutureServerOptions
            .customiseInterceptors
            .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
            .options

        val vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(AppConfig.NumberOfThreads))
        // val vertx  = Vertx.vertx()
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        val interpreter = VertxFutureServerInterpreter(serverOptions)

        Endpoints.endpoints
            .foreach(endpoint =>
                interpreter
                    .blockingRoute(endpoint)
                    .apply(router)
            )

        // Add our documentation endpoints
        Endpoints.docEndpoints
            .foreach(endpoint =>
                interpreter
                    .route(endpoint)
                    .apply(router)
            )

        interpreter.route(Endpoints.metricsEndpoint).apply(router)

        router
            .getRoutes
            .forEach(r => log.atInfo.log(f"Adding route: ${r.methods()}%8s ${r.getPath}%s"))

        val program = server.requestHandler(router).listen(port).asScala

        Await.result(program, Duration.Inf)


// --- Helidon WeServer
//        val serverOptions = NimaServerOptions
//            .customiseInterceptors
//            .serverLog(None)
//            .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
//            .options
//
//        val handler = NimaServerInterpreter(serverOptions).toHandler(Endpoints.allImpl)
//
//        WebServer
//            .builder()
//            .writeBufferSize(131072) // Big buffer to handle full kb tree
//            .routing(builder => builder.any(handler))
//            .port(port)
//            .build()
//            .start()
