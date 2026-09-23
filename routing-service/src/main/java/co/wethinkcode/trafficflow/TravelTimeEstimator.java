package co.wethinkcode.trafficflow;

/**
 * Pure travel-time model so routing behaviour is testable without any HTTP.
 *
 * estimatedMinutes = base + a signal penalty per endpoint + a delay per
 * congestion level. The numbers are deliberately simple — the point is that
 * the estimate is an honest function of congestion + the intersections
 * involved, not a hardcoded value.
 */
final class TravelTimeEstimator {

    static final int BASE_MINUTES = 10;
    static final int MINUTES_PER_CONGESTION_LEVEL = 2;

    private TravelTimeEstimator() {
    }

    static int estimate(IntersectionRecord from, IntersectionRecord to, int congestionLevel) {
        return BASE_MINUTES + signalPenalty(from) + signalPenalty(to)
                + congestionLevel * MINUTES_PER_CONGESTION_LEVEL;
    }

    /** Rough signal delay; unvalidated signal data costs more than a roundabout. */
    private static int signalPenalty(IntersectionRecord intersection) {
        if (intersection.signalType() == null) {
            return 3; // signal type missing upstream — assume the worst
        }
        return switch (intersection.signalType()) {
            case "roundabout" -> 0;
            case "4-way" -> 1;
            case "stop-sign" -> 3;
            case "pedestrian" -> 4;
            default -> 2;
        };
    }
}
