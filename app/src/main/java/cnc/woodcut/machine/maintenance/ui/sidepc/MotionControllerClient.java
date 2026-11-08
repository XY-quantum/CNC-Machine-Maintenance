package cnc.woodcut.machine.maintenance.ui.sidepc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MotionControllerClient {

    public interface StatusListener {
        void onPositionUpdate(double x, double y, double z);
        void onSpindleTemperatureUpdate(double tempC);
        void onError(String errorMessage);
    }

    private StatusListener listener;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean connected = false;

    public void setStatusListener(StatusListener l) {
        this.listener = l;
    }

    public void connect() {
        // establish connection (e.g., TCP/IP, Ethernet) to motion controller
        executor.submit(() -> {
            try {
                // pseudo-code: open socket, negotiate protocol, etc
                Thread.sleep(500); // simulate delay
                connected = true;
                // once connected, you may start periodic polling of status
                startPolling();
            } catch (Exception e) {
                notifyError("Connection failed: " + e.getMessage());
            }
        });
    }

    private void startPolling() {
        executor.submit(() -> {
            while (connected) {
                try {
                    // pseudo-code: request axis positions and spindle temp
                    double x = getAxisPosition("X");
                    double y = getAxisPosition("Y");
                    double z = getAxisPosition("Z");
                    double temp = getSpindleTemperature();
                    if (listener != null) {
                        listener.onPositionUpdate(x, y, z);
                        listener.onSpindleTemperatureUpdate(temp);
                    }
                    Thread.sleep(500); // half second interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    notifyError("Polling error: " + e.getMessage());
                }
            }
        });
    }

    private double getAxisPosition(String axis) {
        // pseudo-implementation: query controller for axis value
        // e.g., send command “?POS X” and parse response
        return Math.random() * 100; // placeholder
    }

    private double getSpindleTemperature() {
        // pseudo-implementation: query spindle temp sensor
        return 30.0 + Math.random() * 10; // placeholder
    }

    public void jog(String axis, double distance) {
        if (!connected) return;
        executor.submit(() -> {
            try {
                // pseudo-code: send jog command, e.g., “JOG X +1.0”
                // send raw command to hardware
                Thread.sleep(50);
            } catch (Exception e) {
                notifyError("Jog error: " + e.getMessage());
            }
        });
    }

    public void setFeedOverride(double factor) {
        if (!connected) return;
        executor.submit(() -> {
            try {
                // pseudo-code: send feed override command “FOVR” factor
                Thread.sleep(20);
            } catch (Exception e) {
                notifyError("Feed override error: " + e.getMessage());
            }
        });
    }

    public void setSpindleOverride(double factor) {
        if (!connected) return;
        executor.submit(() -> {
            try {
                // pseudo-code: send spindle override command “SOVR” factor
                Thread.sleep(20);
            } catch (Exception e) {
                notifyError("Spindle override error: " + e.getMessage());
            }
        });
    }

    public void startProgram() {
        if (!connected) return;
        executor.submit(() -> {
            try {
                // send start program command e.g., “START”
                Thread.sleep(20);
            } catch (Exception e) {
                notifyError("Start program error: " + e.getMessage());
            }
        });
    }

    public void pauseProgram() {
        if (!connected) return;
        executor.submit(() -> {
            try {
                // send pause command
                Thread.sleep(20);
            } catch (Exception e) {
                notifyError("Pause program error: " + e.getMessage());
            }
        });
    }

    public void stopProgram() {
        if (!connected) return;
        executor.submit(() -> {
            try {
                // send stop command
                Thread.sleep(20);
            } catch (Exception e) {
                notifyError("Stop program error: " + e.getMessage());
            }
        });
    }

    private void notifyError(String msg) {
        if (listener != null) listener.onError(msg);
    }

    public void disconnect() {
        connected = false;
        executor.shutdownNow();
        // close connection, cleanup
    }
}

