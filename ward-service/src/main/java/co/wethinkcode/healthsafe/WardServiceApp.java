package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WardServiceApp {

    public static class Ward {
        public String wardId;
        public String wing;
        public String department;
        public Integer bedsAvailable;
        public String notes;

        public Ward() {}

        public Ward(String wardId, String wing, String department, Integer bedsAvailable, String notes) {
            this.wardId = wardId;
            this.wing = wing;
            this.department = department;
            this.bedsAvailable = bedsAvailable;
            this.notes = notes;
        }
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7031);

        app.get("/health", ctx -> ctx.result("OK"));

        // GET /wards{id} - return single ward or 404
        app.get("/wards/{id}", ctx -> {
            String wardId = ctx.pathParam("id").trim().toUpperCase();

            Ward ward = fetchWardsFromIngestion().stream()
                    .filter(w -> w.wardId != null && w.wardId.equalsIgnoreCase(wardId))
                    .findFirst()
                    .orElse(null);

            if (ward == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("Ward not found: " + wardId));
                return;
            }

            ctx.json(ward);
        });

        // GET /wards - return all wards
        app.get("/wards", ctx -> {
            ctx.json(fetchWardsFromIngestion());
        });

        // GET /departments - return unique department names
        app.get("/departments", ctx -> {
            List<String> departments = fetchWardsFromIngestion().stream()
                    .map(w -> w.department)
                    .filter(d -> d != null && !d.isBlank())
                    .distinct()
                    .sorted()
                    .toList();

            ctx.json(departments);
        });
    }

    public static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    private static List<Ward> fetchWardsFromIngestion() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7030/wards"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Failed to fetch wards from ingestion-service: " + response.statusCode());
                return new ArrayList<>();
            }

            ObjectMapper mapper = new ObjectMapper();
            Ward[] wardArray = mapper.readValue(response.body(), Ward[].class);
            List<Ward> wardList = new ArrayList<>();
            for (Ward ward : wardArray) {
                wardList.add(ward);
            }

            System.out.println("Loaded " + wardList.size() + " wards from ingestion-service");
            return wardList;
        } catch (Exception e) {
            System.err.println("Error fetching wards from ingestions-service: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
