package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import static saurav.chandra.baatmessenger.MainActivity.database;
import static saurav.chandra.baatmessenger.MainActivity.storage;
import static saurav.chandra.baatmessenger.MainActivity.uid;

public class ViewContact extends Activity {

    private ImageView photo;
    private TextView name,last_seen,phone_number;
    private String view_contact_uid,view_contact_name,view_contact_phone_number;

    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_view_contact);

        database = ((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/"+uid+"/connected");
        lastOnlineRef = database.getReference("presenceStatus/"+uid+"/lastOnline");

        ActionBar actionbar = getActionBar();
        actionbar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_action_back_white);
        actionbar.setIcon(new ColorDrawable(Color.TRANSPARENT));
        actionbar.setTitle("");

        photo = (ImageView) findViewById(R.id.view_contact_image);
        name = (TextView) findViewById(R.id.view_contact_name);
        last_seen = (TextView) findViewById(R.id.view_contact_last_seen);
        phone_number = (TextView) findViewById(R.id.view_contact_phone_number);

        Intent intent = getIntent();
        view_contact_uid = intent.getStringExtra("view_contact_uid");
        view_contact_name = intent.getStringExtra("view_contact_name");
        view_contact_phone_number = intent.getStringExtra("view_contact_phone_number");

        name.setText(view_contact_name);
        phone_number.setText(view_contact_phone_number);

        StorageReference profilePicRef = storage.getReference("images/profilepic/" + view_contact_uid);

        Glide.with(getApplicationContext())
                .using(new FirebaseImageLoader())
                .load(profilePicRef)
                .crossFade()
                .thumbnail(Glide.with(this).load(R.drawable.image_loader))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(photo);

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ViewPhoto.class);
                intent.putExtra("profile_pic","true");
                intent.putExtra("chat_with_uid",view_contact_uid);
                intent.putExtra("chat_with_name",view_contact_name);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        final DatabaseReference ViewContactUserConnectionsRef = database.getReference("presenceStatus/"+view_contact_uid);
        ViewContactUserConnectionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("connected").getValue().toString().equals("true")) {
                        last_seen.setText("online");
                    } else {
                        long now = System.currentTimeMillis();
                        long timestamp = Long.parseLong(snapshot.child("lastOnline").getValue().toString());
                        last_seen.setText("last seen " + DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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
}