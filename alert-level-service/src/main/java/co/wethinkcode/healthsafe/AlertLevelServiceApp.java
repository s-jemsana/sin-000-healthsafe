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

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        // In-memory alert level state
        AlertLevel currentAlert = new AlertLevel(0);

        // GET /alert-level - return current emergency status
        app.get("/alert-level", ctx -> {
            ctx.json(currentAlert);
        });
    }
}
