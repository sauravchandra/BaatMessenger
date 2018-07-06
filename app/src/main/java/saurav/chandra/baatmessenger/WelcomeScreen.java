package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class WelcomeScreen extends Activity
{

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        ActionBar actionbar= getActionBar();
        actionbar.hide();

        TextView app_policy_tv = (TextView) findViewById(R.id.app_policy_tv);
        app_policy_tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    protected void continueButtonClick(View view) {
        Intent Intent = new Intent(this, RegisterPhone.class);
        startActivity(Intent);
    }
}
