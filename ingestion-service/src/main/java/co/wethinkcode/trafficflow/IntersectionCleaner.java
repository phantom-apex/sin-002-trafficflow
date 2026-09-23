package co.wethinkcode.trafficflow;

import java.util.Locale;
import java.util.Set;

/**
 * Normalizes one raw legacy row into an {@link IntersectionRecord}.
 *
 * The legacy export is deliberately messy: padded and double-spaced fields,
 * inconsistent casing (int-1005 / INT-1005), and a dozen spellings of "yes".
 * Everything ambiguous (blank, N/A, unknown, ...) becomes an explicit null so
 * downstream services can see the gap instead of trusting a guess.
 */
final class IntersectionCleaner {

    private static final Set<String> PLACEHOLDERS = Set.of(
            "", "n/a", "na", "-", "unknown", "tbd", "nan", "null");

    private IntersectionCleaner() {
    }

    /** Cleans one raw row: [intersection_id, district, signal_type, active_flag]. */
    static IntersectionRecord clean(String[] row) {
        return new IntersectionRecord(
                cleanIdentifier(field(row, 0)),
                cleanDistrict(field(row, 1)),
                cleanSignalType(field(row, 2)),
                cleanFlag(field(row, 3)));
    }

    private static String field(String[] row, int index) {
        return index < row.length ? row[index] : "";
    }

    /** Trims the ends and collapses internal runs of spaces to a single space. */
    static String collapseSpaces(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }

    /** True when a field carries no real value (blank or a legacy placeholder). */
    static boolean isPlaceholder(String raw) {
        return PLACEHOLDERS.contains(collapseSpaces(raw).toLowerCase(Locale.ROOT));
    }

    /** IDs: trimmed, single-spaced, uppercase so int-1005 and INT-1005 collide. */
    static String cleanIdentifier(String raw) {
        return collapseSpaces(raw).toUpperCase(Locale.ROOT);
    }

    /** Districts: trimmed, single-spaced, Title Case (e.g. "downtown" -> "Downtown"). */
    static String cleanDistrict(String raw) {
        String value = collapseSpaces(raw);
        if (isPlaceholder(value)) {
            return null;
        }
        StringBuilder titleCased = new StringBuilder();
        for (String word : value.toLowerCase(Locale.ROOT).split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (titleCased.length() > 0) {
                titleCased.append(' ');
            }
            titleCased.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return titleCased.toString();
    }

    /** Signal types: trimmed, single-spaced, lowercase ("4-Way" -> "4-way"); placeholders -> null. */
    static String cleanSignalType(String raw) {
        String value = collapseSpaces(raw);
        if (isPlaceholder(value)) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    /** Flags: Y/yes/1/true -> true, N/no/0/false -> false; anything else -> null, never guessed. */
    static Boolean cleanFlag(String raw) {
        String value = collapseSpaces(raw).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "y", "yes", "1", "true" -> true;
            case "n", "no", "0", "false" -> false;
            default -> null;
        };
    }
}
