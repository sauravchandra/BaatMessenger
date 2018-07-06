package saurav.chandra.baatmessenger;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    private NotificationUtils notificationUtils;

    public static SharedPreferences settings_prefs;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        settings_prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Boolean notif_value = settings_prefs.getBoolean("notification",false);

        if(!notif_value) {

            if (remoteMessage == null)
                return;

        /*// Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification().getBody());
        }*/

            // Check if message contains a data payload.
            if (remoteMessage.getData().size() > 0) {

                try {
                    JSONObject json = new JSONObject(remoteMessage.getData().toString());
                    handleDataMessage(json);
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                }
            }

        }
    }

    /*private void handleNotification(String message) {
        if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
            // app is in foreground, broadcast the push message
            Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
            pushNotification.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

            // play notification sound
            NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
            notificationUtils.playNotificationSound();
        }else{
            // If the app is in background, firebase itself handles the notification
        }
    }*/

    private void handleDataMessage(JSONObject json) {
        try {

            String cw_uid = json.getString("cw_uid");
            String cw_name = json.getString("cw_name");
            String cw_fcmid = json.getString("cw_fcmid");
            String title = json.getString("title");
            String timestamp = json.getString("timestamp");
            String message = json.getString("message");
            int notif_id = strToInt(cw_uid);

            if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
                // app is in foreground, broadcast the push message
                Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
                pushNotification.putExtra("message", message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);
                // play notification sound

                if(settings_prefs.getBoolean("notification_sound",true)) {
                    NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());

                    notificationUtils.playNotificationSound();
                }
            } else {
                // app is in background, show the notification in notification tray
                Intent resultIntent = new Intent(getApplicationContext(), Chat.class);
                resultIntent.putExtra("chat_with_uid", cw_uid);
                resultIntent.putExtra("chat_with_name",cw_name);
                resultIntent.putExtra("chat_with_fcm_id",cw_fcmid);
                resultIntent.putExtra("clear_notification",notif_id);

                showNotificationMessage(getApplicationContext(), title, notif_id, message, timestamp, resultIntent);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Json Exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    /**
     * Showing notification with text only
     */
    private void showNotificationMessage(Context context, String title, int notif_id, String message, String timeStamp, Intent intent) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
        notificationUtils.showNotificationMessage(title, notif_id, message, timeStamp, intent);
    }

    public int strToInt(String string){
        int value=0;
        for (char ch : string.toCharArray()){
            value=value+Character.getNumericValue(ch);
        }
        return value;
    }

}