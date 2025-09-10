package cnc.woodcut.machine.maintenance.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import cnc.woodcut.machine.maintenance.R;

/**
 * Settings:: Notifications
 *            1) Daily/Weekly/Annually Notification
 *            2) upcoming maintenance & alarm to manager
 *            3) Reminder maintenance
 * */
public class SettingsFragment extends Fragment {

    private SwitchCompat notificationToggle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @NonNull ViewGroup container,
                             @NonNull Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container,false);
        notificationToggle = view.findViewById(R.id.notificationToggle);

        notificationToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                scheduleNotification();
            } else {
                cancelScheduleNotification();
            }
        });

        return view;
    }
    /*-* Scheduled Notification I */
    private void scheduleNotification() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                1, TimeUnit.DAYS)
                .setInitialDelay(10, TimeUnit.SECONDS) // for testing
                .build();
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "daily_notification",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }

    /*-* Scheduled Notification II */
    private void cancelScheduleNotification() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("daily_notification");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}