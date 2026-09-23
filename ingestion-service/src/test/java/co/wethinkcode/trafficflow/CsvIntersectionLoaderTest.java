package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvIntersectionLoaderTest {

    private final List<IntersectionRecord> cleaned =
            CsvIntersectionLoader.load("/intersections-legacy.csv");

    @Test
    void collapsesDuplicateIdsToASingleRecord() {
        long int1005Count = cleaned.stream()
                .filter(record -> record.id().equals("INT-1005"))
                .count();
        assertEquals(1, int1005Count);
    }

    @Test
    void mergedDuplicateKeepsFirstNonNullValuePerField() {
        IntersectionRecord first = new IntersectionRecord("INT-1005", "Downtown", null, true);
        IntersectionRecord second = new IntersectionRecord("int-1005", null, "roundabout", null);
        assertEquals(new IntersectionRecord("INT-1005", "Downtown", "roundabout", true),
                CsvIntersectionLoader.merge(first, second));
    }

    @Test
    void everyCleanedRecordKeepsItsIdentity() {
        assertEquals(17, cleaned.size()); // 18 raw rows, one duplicate pair collapsed
        for (IntersectionRecord record : cleaned) {
            assertNotNull(record.id());
            assertTrue(record.id().matches("INT-\\d+"), "unexpected id casing: " + record.id());
        }
    }

    @Test
    void missingFieldsSurviveAsExplicitNullsInTheCleanedList() {
        IntersectionRecord int1007 = cleaned.stream()
                .filter(record -> record.id().equals("INT-1007"))
                .findFirst().orElseThrow();
        assertEquals(null, int1007.signalType());
        assertEquals("Eastside", int1007.district());
        assertEquals(true, int1007.active());
    }
}
