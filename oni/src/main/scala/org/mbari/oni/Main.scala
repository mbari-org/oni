/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni

import io.vertx.core.http.HttpServerOptions
import io.vertx.core.{Vertx, VertxOptions}
import io.vertx.ext.web.Router
import org.mbari.oni.etc.flyway.FlywayMigration
import org.mbari.oni.etc.jdk.Loggers
import org.mbari.oni.etc.jdk.Loggers.given
import sttp.tapir.server.vertx.VertxFutureServerInterpreter.VertxFutureToScalaFuture
import sttp.tapir.server.vertx.{VertxFutureServerInterpreter, VertxFutureServerOptions}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Launches Oni
 */
object Main:

    def main(args: Array[String]): Unit =

        // Set the timezone to UTC to avoid timezone issues with JDBC
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

        FlywayMigration.migrate(AppConfig.DefaultDatabaseConfig) match
            case true  => log.atDebug.log("Database migration complete")
            case false =>
                log.atError.log("Database migration failed. Exiting.")
                System.exit(1)

        val serverOptions = VertxFutureServerOptions
            .customiseInterceptors
            .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
            .options

        val vertx             = Vertx.vertx(new VertxOptions().setWorkerPoolSize(AppConfig.NumberOfThreads))
        val httpServerOptions = new HttpServerOptions().setCompressionSupported(true)
        val server            = vertx.createHttpServer(httpServerOptions)
        val router            = Router.router(vertx)
        val interpreter       = VertxFutureServerInterpreter(serverOptions)

        Endpoints
            .endpoints
            .foreach(endpoint =>
                interpreter
                    .blockingRoute(endpoint)
                    .apply(router)
            )

        // Add our documentation endpoints
        Endpoints
            .docEndpoints
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
