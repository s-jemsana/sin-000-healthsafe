package co.wethinkcode.healthsafe;

import io.javalin.Javalin;


import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStreamReader;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;


public class IngestionServiceApp {

    public static class Ward {
        public String wardId;
        public String wing;
        public String department;
        public Integer bedsAvailable;
        public String notes;

        public Ward(String wardId, String wing, String department, Integer bedsAvailable, String notes) {
            this.wardId = wardId;
            this.wing = wing;
            this.department = department;
            this.bedsAvailable = bedsAvailable;
            this.notes = notes;
        }

        public String getWardId() {
            return wardId;
        }

        public void setWardId(String wardId) {
            this.wardId = wardId;
        }

        public String getWing() {
            return wing;
        }

        public void setWing(String wing) {
            this.wing = wing;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public Integer getBedsAvailable() {
            return bedsAvailable;
        }

        public void setBedsAvailable(Integer bedsAvailable) {
            this.bedsAvailable = bedsAvailable;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));

        // Read and clean CSV on startup
        List<Ward> wards = readAndCleanWards();

        // Expose cleaned records for other services
        app.get("/wards", ctx -> ctx.json(wards));
    }

    private static List<Ward> readAndCleanWards() {
        Map<String, Ward> wardMap = new HashMap<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(
                        IngestionServiceApp.class.getResourceAsStream("/wards-outdated.csv"))).build()) {

                    // Skip header row
                    reader.readNext();

                    // Process data rows
                    String[] record;
                    while ((record = reader.readNext()) != null) {
                        if (record.length < 4) continue;    // Skip incomplete rows

                        try {
                            Ward ward = cleanRow(record);
                            if (ward != null && ward.wardId != null) {
                                String key = ward.wardId;

                                // Prefer wards with valid bedsAvailable
                                if (!wardMap.containsKey(key)) {
                                    wardMap.put(key, ward);
                                } else {
                                    Ward existing = wardMap.get(key);
                                    if (ward.bedsAvailable != null && existing.bedsAvailable == null) {
                                        wardMap.put(key, ward);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing row: " + String.join(",", record));
                            e.printStackTrace();
                        }
                    }
        } catch (Exception e) {
            System.err.println("Error reading CSV: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>(wardMap.values());
    }

    private static Ward cleanRow(String[] record) {
        if (record.length < 4) return null;

        String normalizedWardId = normalizeWardId(record[0]);
        String normalizedWing = normalizeWing(record[1]);
        String normalizedDept = normalizeDepartment(record[2]);
        Integer beds = parseBeds(record[3]);
        String notes = null;

        // Flag data quality issues
        if (beds == null && !isMissingValue(record[3]) && !record[3].trim().isEmpty()) {
            notes = "bedsAvailable was non-numeric ('" + record[3].trim() + "') - flagged for follow-up";
        }
        return new Ward(normalizedWardId, normalizedWing, normalizedDept, beds, notes);
    }

    public static String normalizeWardId(String id) {
        return id.trim().toUpperCase();
    }
    private static String normalizeWing(String wing) {
        if (wing == null || wing.isBlank()) {
            return null;
        }

        // Trim + collapse internal double spaces
        String normalized = wing.trim().replaceAll("\\s+", " ");

        // Capitalize first letter of each word
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return result.toString().trim();
    }

    private static String normalizeDepartment(String dept) {
        if (dept == null || dept.isBlank()) {
            return null;
        }

        // Capitalize first letter of each word
        String normalized = dept.trim().replaceAll("\\s+", " ");
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase()).append(" ");
            }
        }

        String titleCased = result.toString().trim();

        // Normalize spelling variants: "Pediatrics" -> "Paediatrics"
        if (titleCased.equalsIgnoreCase("Pediatrics")) {
            return "Paediatrics";
        }
        return titleCased;
    }

    private static Integer parseBeds(String beds) {
        if (beds == null || beds.isBlank()) {
            return null;
        }

        String trimmed = beds.trim();

        // Check for placeholder values
        if (isMissingValue(trimmed)) {
            return null;
        }

        try {
            int value = Integer.parseInt(trimmed);
            //Reject negative numbers
            if (value < 0) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            // Non-numeric: "five"
            return null;
        }
    }

    private static boolean isMissingValue(String val) {
        if (val == null || val.isBlank()) {
            return true;
        }

        String lower = val.trim().toLowerCase();
        return lower.equals("n/a") ||
                lower.equals("tbd") ||
                lower.equals("unknown") ||
                lower.equals("-") ||
                lower.equals("nan");
    }
}
