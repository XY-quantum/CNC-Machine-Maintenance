package cnc.woodcut.machine.maintenance.ui.sidepc;

public class RemoteControlClient {

    public interface LoginCallback {
        void onLoginResult(boolean success);
    }

    private boolean loggedIn = false;

    public void login(String username, String password, LoginCallback callback) {
        // TODO: implement authentication logic (e.g., REST API, socket, token)
        new Thread(() -> {
            try {
                Thread.sleep(300); // simulate network
                // For demo, accept only a specific test user
                if ("admin".equals(username) && "password".equals(password)) {
                    loggedIn = true;
                    callback.onLoginResult(true);
                } else {
                    callback.onLoginResult(false);
                }
            } catch (InterruptedException e) {
                callback.onLoginResult(false);
            }
        }).start();
    }

    public String getCameraUrlFor(String cameraName) {
        // TODO: return proper URL for the selected camera
        if ("Camera1".equals(cameraName)) {
            return "https://your-machine.local/cam1";
        } else if ("Camera2".equals(cameraName)) {
            return "https://your-machine.local/cam2";
        }
        return "https://your-machine.local/cam1";
    }

    public void connectRemoteSession() {
        if (!loggedIn) return;
        // TODO: open remote session, enable remote commands, streaming etc
    }

    public void disconnect() {
        // TODO: close remote session, logout, free resources
        loggedIn = false;
    }
}

