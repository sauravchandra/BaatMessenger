package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import static saurav.chandra.baatmessenger.MainActivity.uid;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Queue;


public class ChatMediaView extends Activity {

    private ChatMediaAdapter media_adapter;
    private GridView chat_media_grid;

    private FirebaseDatabase database;
    private static ArrayList<ChatMedia> ChatMedia = new ArrayList<>();
    private DatabaseReference chat_media_ref;

    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        setContentView(R.layout.activity_chat_media_view);

        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_userdetails, null);
        TextView actionbar_activity_name = (TextView) actionBarLayout.findViewById(R.id.actionbar_activity_name);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back_white_normal);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF710C")));
        actionbar_activity_name.setText("Media");

        final Intent intent = getIntent();
        String chat_with_uid = intent.getStringExtra("chat_with_uid");
        final String chat_with_name = intent.getStringExtra("chat_with_name");

        database=((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/"+uid+"/connected");
        lastOnlineRef = database.getReference("presenceStatus/"+uid+"/lastOnline");

        chat_media_grid = (GridView) findViewById(R.id.chat_media_grid);
        media_adapter = new ChatMediaAdapter(this, ChatMedia);
        chat_media_grid.setAdapter(media_adapter);
        
        chat_media_ref = database.getReference("messages/"+uid+"_"+chat_with_uid);
        chat_media_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot chatMediaSnapshot) {
                ChatMedia.clear();
                if (chatMediaSnapshot.exists()) {

                    for (DataSnapshot chat_media_snapshot : chatMediaSnapshot.getChildren()) {

                        if (chat_media_snapshot.child("msg_image").getValue().toString().equals("true")) {
                            final String image_url = (String) chat_media_snapshot.child("msg_text").getValue();
                            final String image_caption = (String) chat_media_snapshot.child("photo_caption").getValue();
                            String image_sender;
                            if(chat_media_snapshot.child("msg_sender").getValue().toString().equals(uid)) {
                                image_sender = "You";
                            }
                            else{
                                image_sender = chat_with_name;
                            }
                            final String image_time = (String) chat_media_snapshot.child("msg_time").getValue();

                            ChatMedia.add(new ChatMedia(image_url,image_caption,image_sender,image_time));
                            media_adapter.notifyDataSetChanged();
                            }
                        }
                        chat_media_ref.removeEventListener(this);
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
