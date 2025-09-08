package cnc.woodcut.machine.maintenance.ui.dashboard;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import javax.xml.transform.Result;

import cnc.woodcut.machine.maintenance.R;
import kotlinx.coroutines.scheduling.CoroutineScheduler;

/**
 *  Maintenance Dashboard: Preventative Maintenance Plan, Calendar-based Schedule,
 *                         Notification for upcoming maintenance, Alarm to manager,
 *                         Analytics for machine uptime/downtime
 *                         Sync with cloud or local database(eg, Firebase, MySQL)
 */

public class DashboardFragment extends Fragment {

    private ListView maintenanceList;
    private String[] tasks = {
      "Daily: Clean machine surface",
      "Daily: Check lubrication oil",
      "Weekly: Inspect tool holders",
      "Monthly: Check axis alignment",
      "Quarterly: Replace filters"
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        //maintenanceList = view.findViewById(R.id.maintenanceList);

        /*-- Notification Setup */
        /*OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class).build();
        WorkManager.getInstance(requireContext()).enqueue(workRequest); */

        /*-- Periodic notification */
        /*PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES).build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "dashboard_notification",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
        );*/
        /*-- Notification Channel */


        return view;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //binding = null;
    }
}