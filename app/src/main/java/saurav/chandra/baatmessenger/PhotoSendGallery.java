package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentCallbacks2;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

public class PhotoSendGallery extends Activity {
    private String photo_uri;
    private ImageViewTouch photo_view;
    private ImageView send_button;
    private EditText photo_caption;
    private String uid,chat_with_uid,photo_caption_text,chat_with_name,chat_with_fcm_id,chat_with_phone_number;
    private ImageView actionbar_user_photo_view;

    private FirebaseDatabase database;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference chatWithUserPrivacyRef;
    private StorageReference profilePicRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_photo_send);

        Uri notification = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" + getPackageName() + "/raw/send");
        final Ringtone r = RingtoneManager.getRingtone(this, notification);

        final SharedPreferences settings_prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //Inflate the actionbar and set various details
        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_photo_send, null);
        actionbar_user_photo_view = (ImageView) actionBarLayout.findViewById(R.id.photosend_actionbar_user_photo);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        actionBar.setIcon(new ColorDrawable(Color.TRANSPARENT));
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back_white);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final Intent intent = getIntent();
        photo_uri = intent.getStringExtra("photo_uri");
        uid = intent.getStringExtra("uid");
        chat_with_uid = intent.getStringExtra("chat_with_uid");
        chat_with_name= intent.getStringExtra("chat_with_name");
        chat_with_fcm_id = intent.getStringExtra("chat_with_fcm_id");
        chat_with_phone_number = intent.getStringExtra("chat_with_phone_number");

        database=((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/"+uid+"/connected");
        lastOnlineRef = database.getReference("presenceStatus/"+uid+"/lastOnline");
        profilePicRef = ((BaatMessenger) this.getApplication()).getStorage().getReference().child("images/profilepic/" + chat_with_uid);

        chatWithUserPrivacyRef = database.getReference("users/"+chat_with_uid+"/privacy");
        chatWithUserPrivacyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String profile_pic = dataSnapshot.child("profile_pic").getValue().toString();

                    if (profile_pic.equals("Everyone") || profile_pic.equals("My contacts")) {
                        Glide.with(getApplicationContext())                             //Set chat with user photo
                                .using(new FirebaseImageLoader())
                                .load(profilePicRef)
                                .thumbnail(Glide.with(getApplicationContext()).load(R.drawable.default_dp).transform(new CircleTransform(getApplicationContext())))
                                .crossFade()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transform(new CircleTransform(getApplicationContext()))
                                .into(actionbar_user_photo_view);
                    } else {
                        Glide.with(getApplicationContext())                             //Set default photo
                                .load(R.drawable.default_dp)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transform(new CircleTransform(getApplicationContext()))
                                .into(actionbar_user_photo_view);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        photo_view = (ImageViewTouch) findViewById(R.id.photo_view);
        photo_view.setDisplayType(ImageViewTouchBase.DisplayType.FIT_WIDTH);

        Glide.with(this)
                .load(new File(photo_uri)) // Uri of the picture
                .into(photo_view);

        send_button=(ImageView) findViewById(R.id.sendButton);

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                photo_caption=(EditText) findViewById(R.id.photo_caption);
                photo_caption_text=photo_caption.getText().toString();

                if(settings_prefs.getBoolean("notification_sound",true)) {
                    r.play();
                }

                Intent intent2= new Intent(PhotoSendGallery.this, Chat.class);
                intent2.putExtra("photo_uri",photo_uri);
                intent2.putExtra("photo_caption",photo_caption_text);
                intent2.putExtra("chat_with_uid",chat_with_uid);
                intent2.putExtra("chat_with_name",chat_with_name);
                intent2.putExtra("chat_with_fcm_id",chat_with_fcm_id);
                intent2.putExtra("chat_with_phone_number",chat_with_phone_number);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent2);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent3= new Intent(PhotoSendGallery.this,Chat.class);
        intent3.putExtra("uid",uid);
        intent3.putExtra("chat_with_uid",chat_with_uid);
        intent3.putExtra("chat_with_name",chat_with_name);
        intent3.putExtra("chat_with_fcm_id",chat_with_fcm_id);
        intent3.putExtra("chat_with_phone_number",chat_with_phone_number);
        startActivity(intent3);
        finish();
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
