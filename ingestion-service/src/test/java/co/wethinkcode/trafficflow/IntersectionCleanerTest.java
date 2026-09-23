package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntersectionCleanerTest {

    @Test
    void trimsPaddingAndCollapsesDoubleSpaces() {
        assertEquals("Downtown", IntersectionCleaner.collapseSpaces("  Downtown  "));
        assertEquals("Down town", IntersectionCleaner.collapseSpaces("Down  town"));
        assertEquals("INT-1003", IntersectionCleaner.cleanIdentifier("INT-1003 "));
    }

    @Test
    void normalizesCasingForIdDistrictAndSignalType() {
        assertEquals("INT-1005", IntersectionCleaner.cleanIdentifier("int-1005"));
        assertEquals("Downtown", IntersectionCleaner.cleanDistrict("downtown"));
        assertEquals("Down Town", IntersectionCleaner.cleanDistrict("DOWN TOWN"));
        assertEquals("4-way", IntersectionCleaner.cleanSignalType("4-Way"));
        assertEquals("roundabout", IntersectionCleaner.cleanSignalType("ROUNDABOUT"));
        assertEquals("stop-sign", IntersectionCleaner.cleanSignalType("Stop-Sign"));
    }

    @Test
    void normalizesEveryLegacyBooleanSpelling() {
        assertEquals(Boolean.TRUE, IntersectionCleaner.cleanFlag("Y"));
        assertEquals(Boolean.TRUE, IntersectionCleaner.cleanFlag("yes"));
        assertEquals(Boolean.TRUE, IntersectionCleaner.cleanFlag("1"));
        assertEquals(Boolean.TRUE, IntersectionCleaner.cleanFlag("TRUE"));
        assertEquals(Boolean.FALSE, IntersectionCleaner.cleanFlag("N"));
        assertEquals(Boolean.FALSE, IntersectionCleaner.cleanFlag("no"));
        assertEquals(Boolean.FALSE, IntersectionCleaner.cleanFlag("0"));
        assertEquals(Boolean.FALSE, IntersectionCleaner.cleanFlag("FALSE"));
    }

    @Test
    void leavesUnparseableFlagsAsNullInsteadOfGuessing() {
        assertNull(IntersectionCleaner.cleanFlag("unknown"));
        assertNull(IntersectionCleaner.cleanFlag(""));
        assertNull(IntersectionCleaner.cleanFlag("maybe"));
    }

    @Test
    void mapsMissingAndPlaceholderValuesToExplicitNulls() {
        assertNull(IntersectionCleaner.cleanDistrict(""));
        assertNull(IntersectionCleaner.cleanDistrict("   "));
        assertNull(IntersectionCleaner.cleanSignalType("N/A"));
        assertNull(IntersectionCleaner.cleanSignalType("-"));
        assertNull(IntersectionCleaner.cleanSignalType("unknown"));
        assertNull(IntersectionCleaner.cleanDistrict("TBD"));
    }

    @Test
    void doesNotMistakeRealValuesForPlaceholders() {
        assertFalse(IntersectionCleaner.isPlaceholder("Westside"));
        assertTrue(IntersectionCleaner.isPlaceholder("n/a"));
    }

    @Test
    void cleansTheWorkedExampleRowEndToEnd() {
        IntersectionRecord record = IntersectionCleaner.clean(
                new String[]{"int-1005", " downtown ", "ROUNDABOUT", "TRUE"});
        assertEquals(new IntersectionRecord("INT-1005", "Downtown", "roundabout", true), record);
    }
}
