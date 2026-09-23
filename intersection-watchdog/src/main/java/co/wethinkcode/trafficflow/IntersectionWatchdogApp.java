package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IntersectionWatchdogApp {

    /** Raise the alarm when no heartbeat has been seen for this long. */
    static final long ALERT_AFTER_SECONDS = 15;

    public static void main(String[] args) {
        HeartbeatMonitor monitor = new HeartbeatMonitor();

        ScheduledExecutorService alertChecks = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "alert-checker");
            thread.setDaemon(true);
            return thread;
        });
        alertChecks.scheduleAtFixedRate(() -> logIfAlerting(monitor), 5, 5, TimeUnit.SECONDS);

        Javalin app = Javalin.create().start(7024);

        app.get("/health", ctx -> ctx.result("OK"));

        // Observable alert state: true while the heartbeat is missing or stale.
        app.get("/alert", ctx -> {
            long secondsSince = monitor.secondsSinceLastHeartbeat();
            boolean alert = secondsSince < 0 || secondsSince > ALERT_AFTER_SECONDS;
            ctx.json(Map.of(
                    "alert", alert,
                    "secondsSinceLastHeartbeat", secondsSince,
                    "alertAfterSeconds", ALERT_AFTER_SECONDS,
                    "message", alert
                            ? "Intersection Service is unreachable — routes can no longer be validated"
                            : "Intersection Service is alive"));
        });
    }

    private static void logIfAlerting(HeartbeatMonitor monitor) {
        long secondsSince = monitor.secondsSinceLastHeartbeat();
        if (secondsSince < 0 || secondsSince > ALERT_AFTER_SECONDS) {
            System.err.println("[intersection-watchdog] ALERT: no heartbeat from intersection-service for "
                    + (secondsSince < 0 ? "any known period (never seen)" : secondsSince + "s")
                    + " — routes can no longer be validated");
        }
    }
}
