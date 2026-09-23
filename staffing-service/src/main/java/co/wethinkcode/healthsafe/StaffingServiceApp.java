package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.util.List;

public class StaffingServiceApp {
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

        // TODO (Provides on-call schedules for doctors based on ward and status.)
        // Add domain endpoints for staffing-service here.
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
