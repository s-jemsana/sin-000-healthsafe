package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class StaffingServiceApp {
    private static final String WARD_SERVICE_URL = "http://localhost:7031";
    private static final String ALERT_LEVEL_SERVICE_URL = "http://localhost:7032";

    private static final HttpClient HTTP  = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class Ward {
        public String wardId;
        public String wing;
        public String department;
        public Integer bedsAvailable;
        public String notes;
    }

    public static class AlertLevel {
        public int level;
    }

    public static class StaffingSchedule {
        public String wardId;
        public String wing;
        public String department;
        public int alertLevel;
        public int doctorsRequired;
        public List<String> onCallDoctors;

        public StaffingSchedule(Ward ward, int alertLevel, int doctorsRequired, List<String> onCallDoctors) {
            this.wardId = ward.wardId;
            this.wing = ward.wing;
            this.department = ward.department;
            this.alertLevel = alertLevel;
            this.doctorsRequired = doctorsRequired;
            this.onCallDoctors = onCallDoctors;
        }
    }

    public static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        // GET /staffing/{wardId} - return on-call schedule for a ward
        app.get("/staffing/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId").trim().toUpperCase();

            try {
                Ward ward = fetchWard(wardId);
                AlertLevel alertLevel = fetchAlertLevel();

                ctx.json(buildSchedule(ward, alertLevel.level));
            } catch (WardNotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse("Ward not found: " + wardId));
            } catch (DownstreamServiceException e) {
                ctx.status(503);
                ctx.json(new ErrorResponse(e.getMessage()));
            }
        });
    }

    private static Ward fetchWard(String wardId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WARD_SERVICE_URL + "/wards/" + wardId))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new WardNotFoundException();
            }

            if (response.statusCode() != 200) {
                throw new DownstreamServiceException("ward-service unavailable");
            }

            return MAPPER.readValue(response.body(), Ward.class);
        } catch (WardNotFoundException | DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new DownstreamServiceException("ward-service unavailable");
        }
    }

    private static AlertLevel fetchAlertLevel() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ALERT_LEVEL_SERVICE_URL + "/alert-level"))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new DownstreamServiceException("alert-level-service unavailable");
            }

            return MAPPER.readValue(response.body(), AlertLevel.class);
        } catch (DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new DownstreamServiceException("alert-level-service unavailable");
        }
    }

    private static StaffingSchedule buildSchedule(Ward ward, int alertLevel) {
        int doctorsRequired = calculateDoctorsRequired(ward, alertLevel);
        List<String> onCallDoctors = new ArrayList<>();

        String specialty = ward.department == null || ward.department.isBlank()
                ? "General"
                : ward.department;

        for (int i = 1; i <= doctorsRequired; i++) {
            onCallDoctors.add(specialty + " Doctor " + i);
        }

        return new StaffingSchedule(ward, alertLevel, doctorsRequired, onCallDoctors);
    }

    private static int calculateDoctorsRequired(Ward ward, int alertLevel) {
        int doctors = 1;

        if (alertLevel >= 3) {
            doctors++;
        }

        if (alertLevel >= 6) {
            doctors++;
        }

        if (ward.bedsAvailable != null && ward.bedsAvailable == 0) {
            doctors++;
        }

        return doctors;
    }

    private static class WardNotFoundException extends RuntimeException {
    }

    private static class DownstreamServiceException extends RuntimeException {
        public DownstreamServiceException(String message) {
            super(message);
        }
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
