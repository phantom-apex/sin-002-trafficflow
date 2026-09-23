package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CongestionServiceApp {

    /** The city-wide congestion level: 0 (clear roads) to 8 (gridlock). */
    static final AtomicInteger LEVEL = new AtomicInteger(0);

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        CongestionPublisher publisher = null;
        try {
            publisher = new CongestionPublisher();
        } catch (IllegalStateException e) {
            System.err.println("[congestion-service] " + e.getMessage()
                    + " — running without topic publishing until restart");
        }

        Javalin app = Javalin.create().start(7022);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/congestion", ctx -> ctx.json(Map.of("level", LEVEL.get())));

        // Control-plane entry point: set the current city-wide level. Every
        // accepted change is announced on the congestion-topic so consumers
        // hear about it without polling (see common/README.md).
        CongestionPublisher mq = publisher;
        app.post("/congestion", ctx -> {
            int requested;
            try {
                JsonNode body = JSON.readTree(ctx.body());
                if (body == null || !body.has("level") || !body.get("level").isInt()) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                            .json(Map.of("error", "Body must be {\"level\": <int>}"));
                    return;
                }
                requested = body.get("level").asInt();
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Body must be valid JSON"));
                return;
            }
            if (requested < 0 || requested > 8) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(Map.of("error", "Congestion level must be between 0 and 8"));
                return;
            }
            LEVEL.set(requested);
            if (mq != null) {
                mq.publish(requested);
            }
            ctx.json(Map.of("level", requested));
        });
    }
}
