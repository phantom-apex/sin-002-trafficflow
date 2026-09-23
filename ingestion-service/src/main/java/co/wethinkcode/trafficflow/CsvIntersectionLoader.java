package co.wethinkcode.trafficflow;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the messy legacy CSV export from the classpath, cleans every row via
 * {@link IntersectionCleaner}, and collapses duplicate records for the same
 * real-world intersection (same normalized id, different casing/format).
 */
public final class CsvIntersectionLoader {

    private CsvIntersectionLoader() {
    }

    public static List<IntersectionRecord> load(String classpathResource) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                CsvIntersectionLoader.class.getResourceAsStream(classpathResource), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            Map<String, IntersectionRecord> byId = new LinkedHashMap<>();
            for (String[] row : rows.subList(1, rows.size())) { // row 0 is the header
                IntersectionRecord record = IntersectionCleaner.clean(row);
                IntersectionRecord existing = byId.get(record.id());
                byId.put(record.id(), existing == null ? record : merge(existing, record));
            }
            return List.copyOf(byId.values());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + classpathResource, e);
        } catch (CsvException e) {
            throw new IllegalStateException("Malformed CSV in " + classpathResource, e);
        }
    }

    /**
     * Collapses two records written differently for the same real-world
     * intersection: the first non-null value seen wins for each field.
     */
    static IntersectionRecord merge(IntersectionRecord first, IntersectionRecord second) {
        return new IntersectionRecord(
                first.id(),
                first.district() != null ? first.district() : second.district(),
                first.signalType() != null ? first.signalType() : second.signalType(),
                first.active() != null ? first.active() : second.active());
    }
}
