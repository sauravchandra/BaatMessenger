package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public class UserDet_About extends Activity {

    private static FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_userdet_about);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        mAuth= FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        String uid = currentUser.getUid();

        database=((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/"+uid+"/connected");
        lastOnlineRef = database.getReference("presenceStatus/"+uid+"/lastOnline");

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
}
