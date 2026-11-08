package cnc.woodcut.machine.maintenance.ui.sidepc;

import android.media.RemoteControlClient;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cnc.woodcut.machine.maintenance.R;
import cnc.woodcut.machine.maintenance.ui.sidepc.AlertManager;
import cnc.woodcut.machine.maintenance.ui.sidepc.MotionControllerClient;

public class SidePCControllerFragment extends Fragment {

    // UI – status & control
    private TextView tvAxisPosition, tvSpindleTemp;
    private EditText etFeedOverride, etSpindleOverride;
    private Button btnJogXPos, btnJogXNeg, btnJogYPos, btnJogYNeg, btnJogZPos, btnJogZNeg;
    private Button btnStartJob, btnPauseJob, btnStopJob;

    // UI – remote & cameras
    private Spinner spinnerCameraSelect;
    private WebView webViewCameraFeed;
    private Button btnRemoteConnect;

    // UI – alerts dashboard
    private TextView tvAlertList; // simple alert list, could be ListView/RecyclerView in real app
    private Button btnClearAlerts;

    // UI – authentication/login
    private EditText etUsername, etPassword;
    private Button btnLogin;

    // Clients/interfaces
    private MotionControllerClient motionClient;
    private RemoteControlClient remoteClient;
    private AlertManager alertManager;

    private boolean isAuthenticated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sidepc, container, false);
        initUi(view);
        //setupListeners();
        setupClients();

        return view;
    }

    private void initUi(View view) {
        tvAxisPosition = view.findViewById(R.id.tvAxisPosition);
        tvSpindleTemp = view.findViewById(R.id.tvSpindleTemp);
        etFeedOverride = view.findViewById(R.id.etFeedOverride);
        etSpindleOverride = view.findViewById(R.id.etSpindleOverride);

        btnJogXPos = view.findViewById(R.id.btnJogXPos);
        btnJogXNeg = view.findViewById(R.id.btnJogXNeg);
        btnJogYPos = view.findViewById(R.id.btnJogYPos);
        btnJogYNeg = view.findViewById(R.id.btnJogYNeg);
        btnJogZPos = view.findViewById(R.id.btnJogZPos);
        btnJogZNeg = view.findViewById(R.id.btnJogZNeg);

        btnStartJob = view.findViewById(R.id.btnStartJob);
        btnPauseJob = view.findViewById(R.id.btnPauseJob);
        //btnStopJob = view.findViewById(R.id.btnStopJob);

        spinnerCameraSelect = view.findViewById(R.id.spinnerCameraSelect);
        //webViewCameraFeed = view.findViewById(R.id.webViewCameraFeed);
        btnRemoteConnect = view.findViewById(R.id.btnRemoteConnect);

        tvAlertList = view.findViewById(R.id.tvAlertList);
        btnClearAlerts = view.findViewById(R.id.btnClearAlerts);

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String u = etUsername.getText().toString();
            String p = etPassword.getText().toString();
            try {
                remoteClient.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        btnRemoteConnect.setOnClickListener(v -> {
            if (!isAuthenticated) {
                // show message: need login
                return;
            }
            String cam = spinnerCameraSelect.getSelectedItem().toString();
            //String url = remoteClient.getCameraUrlFor(cam);
            //webViewCameraFeed.loadUrl(url);
            //remoteClient.connectRemoteSession();
        });

        btnClearAlerts.setOnClickListener(v -> {
            alertManager.clearAlerts();
            refreshAlertDisplay();
        });

        // The jog/override/job control listeners remain similar to before,
        // but ensure remoteClient/motionClient only work when authenticated and connected.
        btnJogXPos.setOnClickListener(v -> jogAxis("X", +1));
        btnJogXNeg.setOnClickListener(v -> jogAxis("X", -1));
        // similarly for Y and Z…

        btnStartJob.setOnClickListener(v -> motionClient.startProgram());
        btnPauseJob.setOnClickListener(v -> motionClient.pauseProgram());
        btnStopJob.setOnClickListener(v -> motionClient.stopProgram());

        etFeedOverride.setOnEditorActionListener((v, actionId, event) -> {
            try {
                double percent = Double.parseDouble(v.getText().toString());
                motionClient.setFeedOverride(percent / 100.0);
            } catch (NumberFormatException ignored) {}
            return false;
        });

        etSpindleOverride.setOnEditorActionListener((v, actionId, event) -> {
            try {
                double percent = Double.parseDouble(v.getText().toString());
                motionClient.setSpindleOverride(percent / 100.0);
            } catch (NumberFormatException ignored) {}
            return false;
        });
    }

    private void setupClients() {
        motionClient = new MotionControllerClient();
        //remoteClient = new RemoteControlClient();
        alertManager = new AlertManager(alert -> {
            // callback for new alerts
            appendAlert(alert);
        });

        //motionClient.setStatusListener((x, y, z, temp) -> {
        //updateAxisPositionDisplay(x, y, z);
        // updateSpindleTempDisplay(temp);
        //});

        motionClient.connect();
        alertManager.startMonitoring();
    }

    private void updateAxisPositionDisplay(double x, double y, double z) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                tvAxisPosition.setText(String.format("X: %.2f mm  Y: %.2f mm  Z: %.2f mm", x,y,z))
        );
    }
    private void updateSpindleTempDisplay(double temp) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                tvSpindleTemp.setText(String.format("Spindle Temp: %.1f°C", temp))
        );
    }

    private void jogAxis(String axis, int dir) {
        if (!isAuthenticated) return;
        motionClient.jog(axis, dir * 1.0);
    }

    private void appendAlert(String alertText) {
        getActivity().runOnUiThread(() ->
                tvAlertList.append(alertText + "\n")
        );
    }

    private void refreshAlertDisplay() {
        String all = alertManager.getAllAlertsAsString();
        tvAlertList.setText(all);
    }

    private void enableControls(boolean enabled) {
        // Enable/disable controls after login; you may also switch UI to hide login
        btnRemoteConnect.setEnabled(enabled);
        spinnerCameraSelect.setEnabled(enabled);
        btnStartJob.setEnabled(enabled);
        btnPauseJob.setEnabled(enabled);
        btnStopJob.setEnabled(enabled);
        btnJogXPos.setEnabled(enabled);
        btnJogXNeg.setEnabled(enabled);
        btnJogYPos.setEnabled(enabled);
        btnJogYNeg.setEnabled(enabled);
        btnJogZPos.setEnabled(enabled);
        btnJogZNeg.setEnabled(enabled);
        etFeedOverride.setEnabled(enabled);
        etSpindleOverride.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        motionClient.disconnect();
        //remoteClient.disconnect();
        alertManager.stopMonitoring();
    }
}

