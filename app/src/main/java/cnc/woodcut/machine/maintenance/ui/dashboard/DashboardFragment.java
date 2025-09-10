package cnc.woodcut.machine.maintenance.ui.dashboard;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import cnc.woodcut.machine.maintenance.R;


/**
 *  Maintenance Dashboard: Preventative Maintenance Plan, Calendar-based Schedule,
 *                         Notification for upcoming maintenance, Alarm to manager,
 *                         Analytics for machine uptime/downtime
 *                         Sync with cloud or local database(eg, Firebase, MySQL)
 */

public class DashboardFragment extends Fragment {


    private TextView statusText;
    private ProgressBar loadingIndicator;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusText = view.findViewById(R.id.statusText);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        Button refreshButton = view.findViewById(R.id.refreshButton);

        refreshButton.setOnClickListener(v -> updateDashboardData());
    }

    private void updateDashboardData() {
        loadingIndicator.setVisibility(View.VISIBLE);
        statusText.setText("Updating...");

        // Simulate data fetch with a delay
        new Handler().postDelayed(() -> {
            // Example updated data
            String updatedStatus = "System Status: All systems operational";

            statusText.setText(updatedStatus);
            loadingIndicator.setVisibility(View.GONE);
        }, 2000); // 2-second delay to simulate network call
    }

}