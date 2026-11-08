package cnc.woodcut.machine.maintenance.ui.sidepc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AlertManager {

    public interface AlertListener {
        void onAlert(String alertText);
    }

    private List<String> alerts = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean monitoring = false;
    private AlertListener listener;

    public AlertManager(AlertListener listener) {
        this.listener = listener;
    }

    public void startMonitoring() {
        monitoring = true;
        executor.submit(() -> {
            while (monitoring) {
                try {
                    // pseudo-code: poll sensors or receive push notifications from machine
                    // e.g., tool wear sensor, spindle load sensor, limit switch sensor
                    String maybeAlert = checkForAlert();
                    if (maybeAlert != null) {
                        synchronized (alerts) {
                            alerts.add(maybeAlert);
                        }
                        if (listener != null) listener.onAlert(maybeAlert);
                    }
                    Thread.sleep(1000); // check every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private String checkForAlert() {
        // pseudo-logic: randomly generate an alert for demonstration
        if (Math.random() < 0.01) {  // 1% chance every second
            return "Tool wear alert at " + System.currentTimeMillis();
        }
        return null;
    }

    public void clearAlerts() {
        synchronized (alerts) {
            alerts.clear();
        }
    }

    public String getAllAlertsAsString() {
        StringBuilder sb = new StringBuilder();
        synchronized (alerts) {
            for (String a : alerts) {
                sb.append(a).append("\n");
            }
        }
        return sb.toString();
    }

    public void stopMonitoring() {
        monitoring = false;
        executor.shutdownNow();
    }
}
