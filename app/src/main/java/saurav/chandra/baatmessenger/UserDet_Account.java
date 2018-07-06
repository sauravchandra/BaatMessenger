package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.NotificationManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public class UserDet_Account extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    TextView actionbar_activity_name;

    private static FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference privacyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_userdetails, null);
        actionbar_activity_name = (TextView) actionBarLayout.findViewById(R.id.actionbar_activity_name);

        mAuth= FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        String uid = currentUser.getUid();

        database=((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/"+uid+"/connected");
        lastOnlineRef = database.getReference("presenceStatus/"+uid+"/lastOnline");
        privacyRef = database.getReference("users/"+uid+"/privacy");

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back_white_normal);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF710C")));

        actionbar_activity_name.setText("Account");
        addPreferencesFromResource(R.xml.account_pref);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Preference lastSeenPref = findPreference("last_seen");
        lastSeenPref.setSummary(preferences.getString("last_seen", ""));
        Preference profilePicturePref = findPreference("profile_picture");
        profilePicturePref.setSummary(preferences.getString("profile_picture", ""));
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        Preference delAccountPref = findPreference("delete_account");
        delAccountPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent= new Intent(getApplicationContext(), DeleteAccount.class);
                startActivity(intent);
                return true;
            }

        });

    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            myConnectionsRef.setValue(Boolean.FALSE);
            lastOnlineRef.setValue(ServerValue.TIMESTAMP);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        myConnectionsRef.setValue(Boolean.TRUE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("last_seen")) {
            Preference lastSeenPref = findPreference(key);
            lastSeenPref.setSummary(sharedPreferences.getString(key, ""));

            privacyRef.child("last_seen").setValue(sharedPreferences.getString(key, ""));
        }

        if (key.equals("profile_picture")) {
            Preference profilePicPref = findPreference(key);
            profilePicPref.setSummary(sharedPreferences.getString(key, ""));
            privacyRef.child("profile_pic").setValue(sharedPreferences.getString(key, ""));
        }
    }
}
