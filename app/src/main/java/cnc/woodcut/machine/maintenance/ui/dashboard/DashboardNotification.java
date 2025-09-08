package cnc.woodcut.machine.maintenance.ui.dashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import cnc.woodcut.machine.maintenance.R;

/**
 *  Notification upcoming maintenance & alarm to manager
 *
 *
public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "dashboard_channel",
                    "Dashboard Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = (NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        // Build and show notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "dashboard_channel")
                .setSmallIcon(R.drawable.ic_settings_black_24dp)
                .setContentTitle("Dashboard Update")
                .setContentText("This is a background notification.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        //NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        //notificationManager.notify(1001, builder.build());

        return Result.success();
    }
}
 */
