package cnc.woodcut.machine.maintenance.ui.settings;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.TimeUnit;

import cnc.woodcut.machine.maintenance.MainActivity;
import cnc.woodcut.machine.maintenance.R;
import cnc.woodcut.machine.maintenance.databinding.ActivityMainBinding;

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

        /*-* Send email @ref::github.com/XY-quantum/ */
        FloatingActionButton fabEmail = view.findViewById(R.id.fab_email);

        fabEmail.setOnClickListener(v -> {
            String subject = "MachineX_CHECKLIST_FILE";
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setType("text/plain");
            emailIntent.setData(Uri.parse("mailto:")); // only email apps should handle this

            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MachineX Checklist Report");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "MachineX Report PDF File Attached");

            // start to send
            startActivity(Intent.createChooser(emailIntent, "Send email..."));

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
