package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Looks intersections up on Intersection Service — the validation source of truth. */
final class IntersectionClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    IntersectionClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Returns the record, or null when the intersection does not exist (404). */
    IntersectionRecord get(String id) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/intersections/" + id)).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("intersection-service returned " + response.statusCode());
            }
            return json.readValue(response.body(), IntersectionRecord.class);
        } catch (IOException e) {
            throw new IllegalStateException("intersection-service unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted calling intersection-service", e);
        }
    }
}
