package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;

public class RoutingServiceApp {

    private static final String INTERSECTION_URL =
            System.getenv().getOrDefault("INTERSECTION_URL", "http://localhost:7021");
    private static final String CONGESTION_URL =
            System.getenv().getOrDefault("CONGESTION_URL", "http://localhost:7022");

    public static void main(String[] args) {
        IntersectionClient intersections = new IntersectionClient(INTERSECTION_URL);
        CongestionClient congestion = new CongestionClient(CONGESTION_URL);

        Javalin app = Javalin.create().start(7023);

        app.get("/health", ctx -> ctx.result("OK"));

        // Estimated travel time between two intersections: both endpoints are
        // validated against intersection-service, and the estimate is a
        // function of the live congestion level.
        app.get("/route", ctx -> {
            String from = ctx.queryParam("from");
            String to = ctx.queryParam("to");
            if (from == null || to == null || from.isBlank() || to.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(Map.of("error", "Query parameters 'from' and 'to' are required"));
                return;
            }

            IntersectionRecord start;
            IntersectionRecord end;
            int level;
            try {
                start = intersections.get(from);
                end = intersections.get(to);
                level = congestion.getLevel();
            } catch (IllegalStateException e) {
                ctx.status(HttpStatus.BAD_GATEWAY)
                        .json(Map.of("error", "Upstream dependency unavailable: " + e.getMessage()));
                return;
            }

            if (start == null || end == null) {
                String unknown = start == null ? from : to;
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(Map.of("error", "Unknown intersection: " + unknown));
                return;
            }

            ctx.json(Map.of(
                    "from", start.id(),
                    "to", end.id(),
                    "congestionLevel", level,
                    "estimatedMinutes", TravelTimeEstimator.estimate(start, end, level)));
        });
    }
}
