package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class IngestionServiceApp {

    /** Cleaned once at startup — the legacy export is small and read-only. */
    private static final List<IntersectionRecord> INTERSECTIONS =
            CsvIntersectionLoader.load("/intersections-legacy.csv");

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7020);

        app.get("/health", ctx -> ctx.result("OK"));

        // The cleaned records other services consume as their canonical list.
        app.get("/intersections", ctx -> ctx.json(INTERSECTIONS));

        app.get("/intersections/{id}", ctx -> {
            String id = ctx.pathParam("id");
            IntersectionRecord match = INTERSECTIONS.stream()
                    .filter(record -> record.id().equalsIgnoreCase(id))
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(Map.of("error", "Unknown intersection: " + id));
            } else {
                ctx.json(match);
            }
        });
    }
}
