package saurav.chandra.baatmessenger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.view.Display;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import static saurav.chandra.baatmessenger.MainActivity.uid;

public class ScreenService extends Service {
    public ScreenService() {
    }

    private BroadcastReceiver screenReciever;
    private int mScreenOffInterval = 2000;
    private Handler mScreenOffHandler;
    private int mScreenOnInterval = 2000;
    private Handler mScreenOnHandler;
    private boolean screenOffCheckerRunning;

    @Override
    public void onCreate() {

        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction("saurav.chandra.baatmessenger.SCREEN_OFF");
        this.screenReciever = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                FirebaseDatabase database = ((BaatMessenger) context.getApplicationContext()).getDatabase();
                DatabaseReference myConnectionsRef = database.getReference("presenceStatus/" + uid + "/connected");
                DatabaseReference lastOnlineRef = database.getReference("presenceStatus/" + uid + "/lastOnline");

                if (intent.getAction().equals("saurav.chandra.baatmessenger.SCREEN_OFF")) {

                    myConnectionsRef.setValue(Boolean.FALSE);
                    lastOnlineRef.setValue(ServerValue.TIMESTAMP);
                }

            }
        };

        this.registerReceiver(this.screenReciever, theFilter);
        mScreenOffHandler = new Handler();
        startScreenOffRepeatingTask();
        mScreenOnHandler = new Handler();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Runnable mScreenOffChecker = new Runnable() {
        @Override
        public void run() {
            if (!screenOffCheckerRunning) {
                return;
            }
            if(isScreenOff()){
                Intent intent = new Intent();
                intent.setAction("saurav.chandra.baatmessenger.SCREEN_OFF");
                sendBroadcast(intent);
                stopScreenOffRepeatingTask();
                startScreenOnRepeatingTask();
            }
            mScreenOffHandler.postDelayed(mScreenOffChecker, mScreenOffInterval);
        }
    };

    Runnable mScreenOnChecker = new Runnable() {
        @Override
        public void run() {

            if(!isScreenOff() && !screenOffCheckerRunning){

                startScreenOffRepeatingTask();
                stopScreenOnRepeatingTask();
            }
            mScreenOnHandler.postDelayed(mScreenOnChecker, mScreenOnInterval);
        }
    };


    void startScreenOffRepeatingTask() {
        screenOffCheckerRunning = true;
        mScreenOffChecker.run();
    }

    void stopScreenOffRepeatingTask() {
        mScreenOffHandler.removeCallbacksAndMessages(null);
        screenOffCheckerRunning = false;
    }

    void startScreenOnRepeatingTask() {
        mScreenOnChecker.run();
    }

    void stopScreenOnRepeatingTask() {
        mScreenOnHandler.removeCallbacks(mScreenOnChecker);
    }

    boolean isScreenOff(){

        Boolean state = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {

            DisplayManager dm = (DisplayManager) getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {

                if (display.getState() == Display.STATE_OFF) {
                    state = true;
                }
            }
        }
        else {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager.isScreenOn()){ state = true; }
        }
        return state;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.screenReciever);
    }
}
