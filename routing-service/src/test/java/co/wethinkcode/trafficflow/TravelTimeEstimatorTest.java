package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelTimeEstimatorTest {

    private final IntersectionRecord fourWay =
            new IntersectionRecord("INT-1001", "Downtown", "4-way", true);
    private final IntersectionRecord roundabout =
            new IntersectionRecord("INT-1005", "Downtown", "roundabout", true);

    @Test
    void higherCongestionMeansLongerEstimatedTravelTime() {
        int quiet = TravelTimeEstimator.estimate(fourWay, roundabout, 1);
        int gridlock = TravelTimeEstimator.estimate(fourWay, roundabout, 8);
        assertTrue(gridlock > quiet);
        assertEquals(quiet + 7 * TravelTimeEstimator.MINUTES_PER_CONGESTION_LEVEL, gridlock);
    }

    @Test
    void missingSignalDataCostsMoreThanARoundabout() {
        IntersectionRecord unknownSignal =
                new IntersectionRecord("INT-1007", "Eastside", null, true);
        assertTrue(TravelTimeEstimator.estimate(unknownSignal, roundabout, 0)
                > TravelTimeEstimator.estimate(roundabout, roundabout, 0));
    }

    @Test
    void estimateIsAnHonestFunctionOfItsInputs() {
        int baseline = TravelTimeEstimator.estimate(fourWay, roundabout, 0);
        assertEquals(TravelTimeEstimator.BASE_MINUTES + 1 + 0, baseline);
        assertEquals(baseline, TravelTimeEstimator.estimate(fourWay, roundabout, 0));
    }
}
