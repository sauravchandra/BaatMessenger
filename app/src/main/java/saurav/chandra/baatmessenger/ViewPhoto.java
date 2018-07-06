package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

public class ViewPhoto extends Activity {
    private String photo_uri;
    private ImageViewTouch photo_view;
    private TextView view_photo_caption;
    private String uid,photo_caption,chat_with_uid,chat_with_name;
    private FirebaseDatabase database;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference profilePicRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo);

        database = ((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/"+uid+"/connected");
        lastOnlineRef = database.getReference("presenceStatus/"+uid+"/lastOnline");

        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_layout_viewphoto, null);
        TextView actionbar_viewphoto_name = (TextView) actionBarLayout.findViewById(R.id.actionbar_viewphoto_name);
        TextView actionbar_viewphoto_time = (TextView) actionBarLayout.findViewById(R.id.actionbar_viewphoto_time);

        ActionBar actionbar = getActionBar();
        actionbar.setDisplayShowCustomEnabled(true);
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setCustomView(actionBarLayout);
        actionbar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#99000000")));
        actionbar.setHomeAsUpIndicator(R.drawable.ic_action_back_white);
        actionbar.setIcon(new ColorDrawable(Color.TRANSPARENT));

        Intent intent = getIntent();

        if(intent.hasExtra("profile_pic")){

            if(intent.hasExtra("uid")) {
                uid = intent.getStringExtra("uid");
                actionbar_viewphoto_name.setText("You");
            }

            if(intent.hasExtra("chat_with_uid")){
                chat_with_uid = intent.getStringExtra("chat_with_uid");
                chat_with_name = intent.getStringExtra("chat_with_name");
                actionbar_viewphoto_name.setText(chat_with_name);
            }

            storage = ((BaatMessenger) this.getApplicationContext()).getStorage();
            storageRef = storage.getReference();
            profilePicRef = storageRef.child("images/profilepic_high_res/" + chat_with_uid);

            actionbar_viewphoto_time.setVisibility(View.GONE);

            photo_view = (ImageViewTouch) findViewById(R.id.view_photo);
            photo_view.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

            Glide.with(getApplicationContext())
                    .using(new FirebaseImageLoader())
                    .load(profilePicRef)
                    .thumbnail(Glide.with(this).load(R.drawable.image_loader))
                    .into(photo_view);
        }
        else if(intent.hasExtra("chat_media")){

            if(intent.getStringExtra("chat_media_sender").equals("You")) {
                actionbar_viewphoto_name.setText("You");
            }
            else{
                actionbar_viewphoto_name.setText(intent.getStringExtra("chat_media_sender"));
            }

            actionbar_viewphoto_time.setText(intent.getStringExtra("photo_time"));

            photo_view = (ImageViewTouch) findViewById(R.id.view_photo);
            photo_view.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

            photo_uri = intent.getStringExtra("photo_uri");
            photo_caption = intent.getStringExtra("photo_caption");
            view_photo_caption=(TextView) findViewById(R.id.view_photo_caption);
            view_photo_caption.setGravity(Gravity.CENTER_HORIZONTAL);
            view_photo_caption.setTextSize(18);
            view_photo_caption.setText(photo_caption);

            Glide.with(getApplicationContext())
                    .load(photo_uri) .crossFade()
                    .thumbnail(Glide.with(this).load(R.drawable.spin_loader))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(photo_view);
        }
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
