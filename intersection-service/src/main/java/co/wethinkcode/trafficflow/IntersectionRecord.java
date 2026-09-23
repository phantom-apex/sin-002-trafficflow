package co.wethinkcode.trafficflow;

/**
 * Intersection record as served by IngestionServiceApp's `/intersections`
 * endpoints.
 *
 * Mirrors ingestion-service's own IntersectionRecord — the modules here are
 * independent Maven projects with no shared parent pom, so response-shape
 * classes are duplicated into each consuming service (same convention as
 * `mq.MqConfig`).
 */
public record IntersectionRecord(String id, String district, String signalType, Boolean active) {
}
