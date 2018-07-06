package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

public class UserDetails extends Activity {

    private static FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private StorageReference storageRef;
    private StorageReference profilePicRef;
    private TextView actionbar_activity_name;

    private FirebaseDatabase database;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
        setContentView(R.layout.activity_userdetail);

        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_userdetails, null);
        actionbar_activity_name = (TextView) actionBarLayout.findViewById(R.id.actionbar_activity_name);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back_white_normal);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF710C")));
        actionbar_activity_name.setText("Settings");

        mAuth= FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        final String uid = currentUser.getUid();

        database=((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/"+uid+"/connected");
        lastOnlineRef = database.getReference("presenceStatus/"+uid+"/lastOnline");

        String user_name=currentUser.getDisplayName();
        String user_phone=currentUser.getPhoneNumber();

        TextView user_name_view = (TextView) findViewById(R.id.user_name);
        TextView user_phone_view = (TextView) findViewById(R.id.user_phone);
        ImageView user_photo_view = (ImageView) findViewById(R.id.user_photo);

        FirebaseStorage storage=((BaatMessenger) this.getApplication()).getStorage();

        storageRef = storage.getReference();
        profilePicRef = storageRef.child("images/profilepic_high_res/"+uid);

        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(profilePicRef)
                .thumbnail(Glide.with(this).load(R.drawable.white_loader).transform(new CircleTransform(this)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new CircleTransform(this))
                .into(user_photo_view);

        user_photo_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent2= new Intent(getApplicationContext(), ViewPhoto.class);
                intent2.putExtra("profile_pic","true");
                intent2.putExtra("chat_with_uid",uid);
                intent2.putExtra("chat_with_name","Profile Picture");
                startActivity(intent2);
            }
        });

        user_name_view.setText(user_name);
        user_phone_view.setText(user_phone);

        LinearLayout account,chat,notifications,about;

        account=(LinearLayout) findViewById(R.id.account);
        chat=(LinearLayout) findViewById(R.id.chat);
        notifications=(LinearLayout) findViewById(R.id.notifications);
        about=(LinearLayout) findViewById(R.id.about);

        account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserDet_Account.class);
                startActivity(intent);
            }
        });

        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserDet_Chat.class);
                startActivity(intent);
            }
        });

        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserDet_Notifications.class);
                startActivity(intent);
            }
        });

        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserDet_About.class);
                startActivity(intent);
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