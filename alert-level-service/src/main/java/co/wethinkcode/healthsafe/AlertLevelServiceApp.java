package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

public class AlertLevelServiceApp {

    public static class AlertLevel {
        public int level;

        public AlertLevel(int level) {
            this.level = level;
        }

        public AlertLevel() {
            this.level = 0;     // Default: no emergency
        }
    }

    public static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        // In-memory alert level state
        AlertLevel currentAlert = new AlertLevel(0);

        // GET /alert-level - return current emergency status
        app.get("/alert-level", ctx -> {
            ctx.json(currentAlert);
        });

        // POST /alert-level - update emergency status with validation
        app.post("/alert-level", ctx -> {
            try {
                AlertLevel newAlert = ctx.bodyAsClass(AlertLevel.class);

                // Validate: level must be 0-8
                if (newAlert.level < 0 || newAlert.level > 8) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("Invalid alert level: " + newAlert.level + ". Must be 0-8"));
                    return ;
                }

                currentAlert.level = newAlert.level;
                ctx.status(200);
                ctx.json(currentAlert);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Invalid request: " + e.getMessage()));
            }
        });
    }
}
