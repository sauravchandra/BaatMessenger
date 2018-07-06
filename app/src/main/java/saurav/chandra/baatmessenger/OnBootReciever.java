package saurav.chandra.baatmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceLauncher = new Intent(context, NotificationService.class);
            context.startService(serviceLauncher);

        }
    }
}
