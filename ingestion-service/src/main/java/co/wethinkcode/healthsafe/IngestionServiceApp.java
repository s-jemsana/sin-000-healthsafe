package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.util.List;
import java.util.ArrayList;

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
        return new ArrayList<>();
    };
    private static Ward cleanRow(String[] record) {
        return null;
    };
    private static String normalizeWing(String wing) {
        return null;
    };
    private static String normalizeDepartment(String dept) {
        return null;
    };
    private static boolean isMissingValue(String val) {
        return false;
    };

}
