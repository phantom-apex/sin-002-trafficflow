package co.wethinkcode.trafficflow;

/**
 * One cleaned intersection record.
 *
 * district/signalType/active are null when the legacy export had no usable
 * value — missing data is preserved explicitly, never guessed or dropped.
 */
public record IntersectionRecord(String id, String district, String signalType, Boolean active) {
}
