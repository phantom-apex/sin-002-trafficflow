package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Loads the canonical intersection list from IngestionServiceApp at startup.
 * Intersection Service is the source of truth for validation, but it does not
 * own the raw data — ingestion does.
 */
public final class CanonicalIntersections {

    private final List<IntersectionRecord> intersections;

    private CanonicalIntersections(List<IntersectionRecord> intersections) {
        this.intersections = intersections;
    }

    /**
     * Fetches the cleaned records from ingestion-service. Returns an empty
     * holder when ingestion is unreachable so the service still boots (its
     * {@link #isLoaded()} flag lets callers/reporting see the gap).
     */
    public static CanonicalIntersections fetchFrom(String ingestionBaseUrl) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ingestionBaseUrl + "/intersections"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("ingestion-service returned " + response.statusCode());
            }
            List<IntersectionRecord> parsed = new ObjectMapper().readValue(
                    response.body(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<IntersectionRecord>>() {
                    });
            return new CanonicalIntersections(parsed);
        } catch (Exception e) {
            System.err.println("[intersection-service] could not load canonical list from "
                    + ingestionBaseUrl + ": " + e.getMessage());
            return new CanonicalIntersections(List.of());
        }
    }

    public boolean isLoaded() {
        return !intersections.isEmpty();
    }

    public List<IntersectionRecord> all() {
        return intersections;
    }

    /** Case-insensitive lookup by cleaned id (INT-1005 == int-1005). */
    public IntersectionRecord byId(String id) {
        return intersections.stream()
                .filter(record -> record.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    /** Case-insensitive lookup by cleaned district name. */
    public List<IntersectionRecord> byDistrict(String district) {
        String wanted = district.trim().toLowerCase(java.util.Locale.ROOT);
        return intersections.stream()
                .filter(record -> record.district() != null
                        && record.district().toLowerCase(java.util.Locale.ROOT).equals(wanted))
                .toList();
    }
}
