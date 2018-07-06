package saurav.chandra.baatmessenger;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.app.Notification.PRIORITY_HIGH;
import static android.app.Notification.PRIORITY_MAX;
import static android.content.Context.MODE_PRIVATE;


public class NotificationUtils {

    private SharedPreferences notificationPrefs;

    private static String TAG = NotificationUtils.class.getSimpleName();

    private Context mContext;

    protected NotificationUtils(Context mContext) {
        this.mContext = mContext;
    }

    protected void showNotificationMessage(final String title, final int notif_id, final String message, final String timeStamp, Intent intent) {
        // Check for empty push message
        if (TextUtils.isEmpty(message))
            return;

        // notification icon
        final int icon = R.mipmap.logo;

        final PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        0,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                );

        final Notification.Builder mBuilder = new Notification.Builder(mContext);


        final Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + mContext.getPackageName() + "/raw/notification");

        showSmallNotification(mBuilder,notif_id, icon, title, message, timeStamp, resultPendingIntent, alarmSound);
    }


    private void showSmallNotification(Notification.Builder mBuilder,int notif_id, int icon, String title, String message, String timeStamp, PendingIntent resultPendingIntent, Uri alarmSound) {

        notificationPrefs = mContext.getSharedPreferences("notification", MODE_PRIVATE);

        Intent intent = new Intent(mContext, ResetNotificationNumber.class);
        intent.putExtra("id", notif_id);
        PendingIntent resetNotifIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        if (notificationPrefs.getString(String.valueOf(notif_id), "0").equals("0")) {

            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();

            inboxStyle.addLine(message);

            Notification notification;
            mBuilder.setSmallIcon(icon).setTicker(title)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(resultPendingIntent)
                    .setSound(alarmSound)
                    .setStyle(inboxStyle)
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.logo)
                    .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), icon))
                    .setPriority(PRIORITY_MAX)
                    .setLights(Color.MAGENTA, 6000,3000)
                    .setDeleteIntent(resetNotifIntent);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            if (prefs.getBoolean("vibrate", true)) {
                mBuilder.setVibrate(new long[]{0, 350, 350, 350, 350});
            } else {
                mBuilder.setVibrate(new long[]{0L});
            }

            notification = mBuilder.build();

            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notif_id, notification);
            notificationPrefs.edit().putString(String.valueOf(notif_id), String.valueOf(Integer.valueOf(notificationPrefs.getString(String.valueOf(notif_id), "0"))+2)).apply();

        }
        else{

            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();

            Notification notification;
            mBuilder.setSmallIcon(icon).setTicker(title)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(notificationPrefs.getString(String.valueOf(notif_id), "0")+" new messages")
                    .setContentIntent(resultPendingIntent)
                    .setSound(alarmSound)
                    .setStyle(inboxStyle)
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.logo)
                    .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), icon))
                    .setPriority(PRIORITY_MAX)
                    .setLights(Color.MAGENTA, 6000,3000)
                    .setDeleteIntent(resetNotifIntent);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            if (prefs.getBoolean("vibrate", true)) {
                mBuilder.setVibrate(new long[]{0, 350, 350, 350, 350});
            } else {
                mBuilder.setVibrate(new long[]{0L});
            }

            notification = mBuilder.build();

            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notif_id, notification);
            notificationPrefs.edit().putString(String.valueOf(notif_id), String.valueOf(Integer.valueOf(notificationPrefs.getString(String.valueOf(notif_id), "0"))+1)).apply();
        }
    }


    // Playing notification sound
    protected void playNotificationSound() {
        try {
            Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" + mContext.getPackageName() + "/raw/recieve");
            Ringtone r = RingtoneManager.getRingtone(mContext, alarmSound);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method checks if the app is in background or not
     */
    protected static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    // Clears notification tray messages
    protected static void clearNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static long getTimeMilliSec(String timeStamp) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = format.parse(timeStamp);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}