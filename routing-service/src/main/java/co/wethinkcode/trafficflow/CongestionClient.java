package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Fetches the current city-wide congestion level from Congestion Service. */
final class CongestionClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    CongestionClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    int getLevel() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/congestion")).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("congestion-service returned " + response.statusCode());
            }
            return json.readTree(response.body()).get("level").asInt();
        } catch (IOException e) {
            throw new IllegalStateException("congestion-service unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted calling congestion-service", e);
        }
    }
}
