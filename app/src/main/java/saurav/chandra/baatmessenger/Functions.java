package saurav.chandra.baatmessenger;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.support.v4.content.PermissionChecker.checkSelfPermission;

public class Functions {

    protected static Boolean askForPermission(String[] permission, Integer requestCode, Activity activity) {
        Boolean permission_state_granted = false;

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String perm_element : permission) {

            if (checkSelfPermission(activity, perm_element) != PackageManager.PERMISSION_GRANTED) {

                listPermissionsNeeded.add(perm_element);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
        }

        return permission_state_granted;
    }

    protected static Boolean checkPermission(String[] permission, Activity activity) {
        Boolean permission_state_granted = false;

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String perm_element : permission) {

            if (checkSelfPermission(activity, perm_element) != PackageManager.PERMISSION_GRANTED) {

                listPermissionsNeeded.add(perm_element);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            permission_state_granted = true;
        }

        return permission_state_granted;
    }

    protected static String getTime(){
        Calendar c = Calendar.getInstance();
        String messageTimeAmPmSet;
        String messageTimeHour = ""+c.get(Calendar.HOUR);
        if(messageTimeHour.equals("0")){
            messageTimeHour="12";
        }

        String messageTimeMinute=""+c.get(Calendar.MINUTE);
        if(Integer.parseInt(messageTimeMinute)/10==0){
            messageTimeMinute="0"+messageTimeMinute;
        }
        int messageTimeAmPm=c.get(Calendar.AM_PM);
        if(messageTimeAmPm == Calendar.AM){
            messageTimeAmPmSet="AM";
        }
        else
        {
            messageTimeAmPmSet="PM";
        }

        return messageTimeHour+":"+messageTimeMinute+" "+messageTimeAmPmSet;
    }

    protected static Bitmap getclip(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);

        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth()/2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}
