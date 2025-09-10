package cnc.woodcut.machine.maintenance.ui.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/*-* Scheduled Notification */
public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationUtils.sendNotification(getApplicationContext(),
                "Reminder", "This is your scheduled notification.");
        return Result.success();
    }
}
