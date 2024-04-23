package org.mbari.oni.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.mbari.oni.AppConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Health check at ROOT/q/health. We report some info about the server
 */
@Liveness
@ApplicationScoped
public class ServerHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        var runtime = Runtime.getRuntime();
        var response =  HealthCheckResponse.named("Server status")
                .up()
                .withData("jdkVersion", Runtime.version().toString())
                .withData("availableProcessors", runtime.availableProcessors())
                .withData("freeMemory", runtime.freeMemory())
                .withData("maxMemory", runtime.maxMemory())
                .withData("totalMemory", runtime.totalMemory())
                .withData("application", AppConfig.NAME)
                .withData("version", AppConfig.VERSION)
                .withData("description", AppConfig.DESCRIPTION);
        try {
            var hostname = InetAddress.getLocalHost().getHostName();
            response.withData("hostname", hostname);
        }
        catch (UnknownHostException e) {
            // nothing to do
        }
        return response.build();
    }
}