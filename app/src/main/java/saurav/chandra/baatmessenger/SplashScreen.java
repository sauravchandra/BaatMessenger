package saurav.chandra.baatmessenger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 800;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        prefs = getSharedPreferences(Config.SHARED_PREF, MODE_PRIVATE);
        Boolean user_details_set = prefs.getBoolean("user_detail_set",false);
        if (FirebaseAuth.getInstance().getCurrentUser()!=null) {
            if(user_details_set){
                Intent Intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(Intent);
                finish();
            }
            else {
                Intent Intent = new Intent(getApplicationContext(), SetupProfile.class);
                startActivity(Intent);
                finish();
            }
        } else {
            setContentView(R.layout.activity_splash);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setContentView(R.layout.activity_welcome_screen);
                }
            }, SPLASH_TIME_OUT);
        }

    }


    public void continueButtonClick(View view) {
        Intent Intent = new Intent(this, RegisterPhone.class);
        startActivity(Intent);
    }

}