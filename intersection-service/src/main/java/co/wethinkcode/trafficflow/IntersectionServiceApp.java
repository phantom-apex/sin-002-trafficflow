package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;

public class IntersectionServiceApp {

    private static final String INGESTION_URL =
            System.getenv().getOrDefault("INGESTION_URL", "http://localhost:7020");

    public static void main(String[] args) {
        CanonicalIntersections canonical = CanonicalIntersections.fetchFrom(INGESTION_URL);
        System.out.println("[intersection-service] loaded " + canonical.all().size()
                + " canonical intersections from ingestion-service");

        Javalin app = Javalin.create().start(7021);

        app.get("/health", ctx -> ctx.result(canonical.isLoaded() ? "OK" : "DEGRADED"));

        app.get("/intersections", ctx -> ctx.json(canonical.all()));

        // Validation lookups — the source of truth other services call.
        app.get("/intersections/{id}", ctx -> {
            IntersectionRecord match = canonical.byId(ctx.pathParam("id"));
            if (match == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(Map.of("error", "Unknown intersection: " + ctx.pathParam("id")));
            } else {
                ctx.json(match);
            }
        });

        app.get("/districts/{name}", ctx -> {
            var matches = canonical.byDistrict(ctx.pathParam("name"));
            if (matches.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(Map.of("error", "Unknown district: " + ctx.pathParam("name")));
            } else {
                ctx.json(matches);
            }
        });
    }
}
