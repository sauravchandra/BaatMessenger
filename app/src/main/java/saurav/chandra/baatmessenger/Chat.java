package saurav.chandra.baatmessenger;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.Html;

import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import saurav.chandra.baatmessenger.emoji.Emojicon;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.Toast;

import static android.Manifest.permission.CAMERA;
import static saurav.chandra.baatmessenger.Functions.getTime;

public class Chat extends Activity {

    //ActionBar variable deceleration
    private static TextView actionbar_user_name, actionbar_user_status;
    private ImageView actionbar_user_photo_view;

    //Main content area variable deceleration
    private static LinearLayout layout;
    private ImageView sendButton, emojiButton, photoButton, fileButton;
    private EmojiconEditText messageArea;
    private ScrollView scrollView;

    //Getting current user varaible deceleration
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String uid, current_user_name, current_user_phone_number, current_user_fcm_id, chat_with_uid, chat_with_name, chat_with_fcm_id, chat_with_phone_number;

    private String downloadUrl;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference profilePicRef;

    private FirebaseDatabase database;
    private DatabaseReference chatWithUserConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference mylastOnlineRef;
    private DatabaseReference userTypingRef;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference chatMediaStorage;
    private DatabaseReference chatWithUserPrivacyRef;
    private DatabaseReference chatWithUserTypingRef;

    private DatabaseReference chat_ref_1, chat_ref_2, last_msg_ref_1, last_msg_ref_2;
    private ChildEventListener chat_listener;
    private Query chat_msg_ref1_query, chat_msg_ref2_query;
    private SharedPreferences settings_prefs, prefs;

    private View rootView;
    private DisplayMetrics displayMetrics;
    private int width;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        setContentView(R.layout.activity_chat);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        database = ((BaatMessenger) this.getApplication()).getDatabase();

        //Get Intent from fragments
        Intent intent = getIntent();
        chat_with_uid = intent.getStringExtra("chat_with_uid");
        chat_with_name = intent.getStringExtra("chat_with_name");
        chat_with_fcm_id = intent.getStringExtra("chat_with_fcm_id");
        chat_with_phone_number = intent.getStringExtra("chat_with_phone_number");
        if (intent.hasExtra("clear_notification")) {
            SharedPreferences sharedPreferences = getSharedPreferences("notification", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(String.valueOf(intent.getExtras().getInt("clear_notification")));
            editor.apply();
        }

        //Get the current user details
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        uid = currentUser.getUid();
        current_user_name = currentUser.getDisplayName();
        current_user_phone_number = currentUser.getPhoneNumber();

        settings_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = getSharedPreferences(Config.SHARED_PREF, MODE_PRIVATE);
        current_user_fcm_id = prefs.getString("regId", null);

        //Get a refernce to Firebase storage
        storage = ((BaatMessenger) this.getApplication()).getStorage();
        storageRef = storage.getReference();

        //Get chat with user profile picture
        profilePicRef = storageRef.child("images/profilepic/" + chat_with_uid);

        chatWithUserConnectionsRef = database.getReference("presenceStatus/" + chat_with_uid + "/connected"); //Is the chat with user online?
        myConnectionsRef = database.getReference("presenceStatus/" + uid + "/connected");

        lastOnlineRef = database.getReference("presenceStatus/" + chat_with_uid + "/lastOnline"); //If not, when was he last online?
        mylastOnlineRef = database.getReference("presenceStatus/" + uid + "/lastOnline");

        userTypingRef = database.getReference("typingStatus/" + uid + "/" + uid); //Are you typing?
        chatWithUserTypingRef = database.getReference("typingStatus/" + uid + "/" + chat_with_uid); //Is the user you are talking with typing?

        chatWithUserPrivacyRef = database.getReference("privacyStatus/" + chat_with_uid );


        //Inflate the actionbar and set various details
        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_chat, null);
        actionbar_user_photo_view = (ImageView) actionBarLayout.findViewById(R.id.actionbar_user_photo);
        actionbar_user_name = (TextView) actionBarLayout.findViewById(R.id.actionbar_user_name);
        actionbar_user_status = (TextView) actionBarLayout.findViewById(R.id.actionbar_user_status);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ffffff")));

        actionbar_user_name.setText(chat_with_name); //Set chat with user name

        actionbar_user_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ViewContact.class);
                intent.putExtra("view_contact_uid", chat_with_uid);
                intent.putExtra("view_contact_name", chat_with_name);
                intent.putExtra("view_contact_phone_number", chat_with_phone_number);
                startActivity(intent);
            }
        });

        actionbar_user_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ViewContact.class);
                intent.putExtra("view_contact_uid", chat_with_uid);
                intent.putExtra("view_contact_name", chat_with_name);
                intent.putExtra("view_contact_phone_number", chat_with_phone_number);
                startActivity(intent);
            }
        });

        Uri notification = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/send");
        final Ringtone r = RingtoneManager.getRingtone(this, notification);

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
        myConnectionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                final DatabaseReference user_exists_ref = database.getReference("users/" + uid);
                user_exists_ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot user_exists_snapshot) {
                        if (user_exists_snapshot.exists()) {
                            if (!snapshot.exists()) {
                                myConnectionsRef.setValue(Boolean.TRUE);

                                // when this device disconnects, remove it
                                myConnectionsRef.onDisconnect().removeValue();

                                // when I disconnect, update the last time I was seen online
                                mylastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Error...");
            }
        });


        //getPresenceStatus(); //Now get the chat with user presence status

        //Attaching a listener to check when chat with user starts typing
        chatWithUserTypingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.getValue().equals("true")) {
                        actionbar_user_status.setText("typing...");
                    }
                    if (snapshot.getValue().equals("false")) {
                        actionbar_user_status.setText("");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //If the chat with user stops typing, display user presence status again
        actionbar_user_status.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (actionbar_user_status.getText().toString().equals("")) {
                    getPresenceStatus();
                }
            }
        });

        //Now coming to the main content layout
        layout = (LinearLayout) findViewById(R.id.layout1);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        messageArea = (EmojiconEditText) findViewById(R.id.messageArea);
        sendButton = (ImageView) findViewById(R.id.sendButton);
        emojiButton = (ImageView) findViewById(R.id.emoji_button);

        messageArea.setEmojiconSize(dptopix(22));
        //Fire current user is typing event
        messageArea.addTextChangedListener(new TextWatcher() {

            private Timer timer = new Timer();
            private final long DELAY = 700;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (timer != null)
                    timer.cancel();
            }

            @Override
            public void afterTextChanged(Editable s) {
                userTypingRef.setValue("true");
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        userTypingRef.setValue("false");
                    }

                }, DELAY);
            }
        });

        rootView = findViewById(R.id.root_view);

        displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

        String wallpaper = prefs.getString("wallpaper", "default");
        if (wallpaper.equals("default")) {
            rootView.setBackgroundColor(Color.parseColor("#E4DCD3"));
        } else {
            File file = new File(wallpaper);
            if (file.exists()) {
                Bitmap bmp2 = BitmapFactory.decodeFile(file.getAbsolutePath());
                Drawable wallpaper_db = new BitmapDrawable(getResources(), scaleCenterCrop(bmp2, height - 200, width));
                rootView.setBackground(wallpaper_db);
            } else {
                rootView.setBackgroundColor(Color.parseColor("#E4DCD3"));

            }
        }

        final EmojiconsPopup popup = new EmojiconsPopup(rootView, this);

        //Will automatically set size according to the soft keyboard size
        popup.setSizeForSoftKeyboard();

        //If the emoji popup is dismissed, change emojiButton to smiley icon
        popup.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss() {
                changeEmojiKeyboardIcon(emojiButton, R.drawable.emoji_button);
            }
        });

        //If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {
                if (popup.isShowing())
                    popup.dismiss();
            }
        });

        //On emoji clicked, add it to edittext
        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                if (messageArea == null || emojicon == null) {
                    return;
                }

                int start = messageArea.getSelectionStart();
                int end = messageArea.getSelectionEnd();
                if (start < 0) {
                    messageArea.append(emojicon.getEmoji());
                } else {
                    messageArea.getText().replace(Math.min(start, end),
                            Math.max(start, end), emojicon.getEmoji(), 0,
                            emojicon.getEmoji().length());
                }
            }
        });

        //On backspace clicked, emulate the KEYCODE_DEL key event
        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {
                KeyEvent event = new KeyEvent(
                        0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                messageArea.dispatchKeyEvent(event);
            }
        });

        photoButton = (ImageView) findViewById(R.id.photo_button);
        fileButton = (ImageView) findViewById(R.id.file_button);

        chat_ref_1 = database.getReference("messages/" + uid + "/" + chat_with_uid);
        chat_ref_2 = database.getReference("messages/" + chat_with_uid + "/" + uid);
        last_msg_ref_1 = database.getReference("lastMessage/" + uid + "/" + chat_with_uid);
        last_msg_ref_2 = database.getReference("lastMessage/" + chat_with_uid + "/" + uid);

        chat_ref_1.keepSynced(true);
        chat_ref_2.keepSynced(true);

        chat_msg_ref1_query = chat_ref_1.limitToLast(100);
        chat_msg_ref2_query = chat_ref_2.limitToLast(100);

        chat_listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.exists()) {
                    final String msg_id = dataSnapshot.getKey();
                    Map map = (Map) dataSnapshot.getValue();

                    String msg_uid = null;
                    String msg_sender_uid = null;
                    String message = null;
                    String message_time = null;
                    String msg_state = null;
                    String isImage = null;

                    if (map != null) {
                        if (map.get("msg_uid") != null) {
                            msg_uid = map.get("msg_uid").toString();
                        }
                        if (map.get("msg_sender") != null) {
                            msg_sender_uid = map.get("msg_sender").toString();
                        }
                        if (map.get("msg_text") != null) {
                            message = map.get("msg_text").toString();
                        }
                        if (map.get("msg_time") != null) {
                            message_time = map.get("msg_time").toString();
                        }
                        if (map.get("msg_state") != null) {
                            msg_state = map.get("msg_state").toString();
                        }
                        if (map.get("msg_image") != null) {
                            isImage = map.get("msg_image").toString();
                        }

                        String photo_caption = null;
                        if (isImage != null && isImage.equals("true")) {
                            photo_caption = map.get("photo_caption").toString();
                        }

                        if (msg_sender_uid != null) {
                            if (msg_sender_uid.equals(uid)) {

                                last_msg_ref_1.child("unread_msg").setValue("0");

                                if (isImage != null) {
                                    if (isImage.equals("true")) {
                                        addImageBox(message, photo_caption, message_time, 1, msg_state, getTextSize(), msg_id, msg_uid);
                                    } else {
                                        addMessageBox(message, message_time, 1, msg_state, getTextSize(), msg_id, msg_uid);
                                    }
                                }

                            } else {
                                if (isImage != null) {
                                    if (isImage.equals("true")) {
                                        addImageBox(message, photo_caption, message_time, 2, msg_state, getTextSize(), msg_id, msg_uid);
                                    } else {
                                        addMessageBox(message, message_time, 2, msg_state, getTextSize(), msg_id, msg_uid);
                                    }


                                    chat_ref_1.child(msg_id).child("msg_state").setValue("3").addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            chat_ref_2.child(msg_id).child("msg_state").setValue("3").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    last_msg_ref_1.child("msg_state").setValue("3");
                                                    last_msg_ref_1.child("unread_msg").setValue("0");
                                                }
                                            });
                                        }
                                    });
                                }


                            }
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(com.google.firebase.database.DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        if (intent.hasExtra("photo_uri")) {

            final String photo_uri = intent.getStringExtra("photo_uri");
            final String photo_caption = intent.getStringExtra("photo_caption");
            Uri file = Uri.fromFile(new File(photo_uri));

            //Current user last message
            last_msg_ref_1.child("last_modified").setValue(-1 * System.currentTimeMillis());
            last_msg_ref_1.child("uid").setValue(chat_with_uid);
            last_msg_ref_1.child("phone_number").setValue(chat_with_phone_number);
            last_msg_ref_1.child("name").setValue(chat_with_name);
            last_msg_ref_1.child("sender").setValue(uid);
            last_msg_ref_1.child("msg_text").setValue("Photo");
            last_msg_ref_1.child("msg_time").setValue(getTime());
            last_msg_ref_1.child("msg_state").setValue("1");
            last_msg_ref_1.child("msg_image").setValue("true");
            last_msg_ref_1.child("fcm_id").setValue(chat_with_fcm_id);

            final DatabaseReference unread_msg_ref1 = last_msg_ref_1.child("unread_msg");
            unread_msg_ref1.setValue("0");
            unread_msg_ref1.keepSynced(true);

            /*unread_msg_ref1.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if(dataSnapshot.exists()) {
                        if (!String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString())).equals("0")) {
                            unread_msg_ref1.setValue(String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString()) + 1));
                        } else {
                            unread_msg_ref1.setValue("1");
                        }
                    }
                    else {
                        unread_msg_ref1.setValue("1");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });*/

            //Chat with user message
            Map<String, Object> map2 = new HashMap<String, Object>();
            final Random rn = new Random();
            map2.put("msg_uid", System.currentTimeMillis() + rn.nextInt(100));
            map2.put("msg_text", "default");
            map2.put("msg_time", getTime());
            map2.put("msg_state", "1");
            map2.put("msg_sender", uid);
            map2.put("photo_caption", photo_caption);
            map2.put("msg_image", "true");

            final DatabaseReference msgSendRef2 = chat_ref_2.push();

            msgSendRef2.setValue(map2).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    msgSendRef2.child("msg_state").setValue("1");
                    last_msg_ref_1.child("msg_state").setValue("1");
                }
            });

            //Current user message
            chat_ref_1.child(msgSendRef2.getKey()).setValue(map2).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    //Chat with user last message
                    last_msg_ref_2.child("last_modified").setValue(-1 * System.currentTimeMillis());
                    last_msg_ref_2.child("uid").setValue(uid);
                    last_msg_ref_2.child("phone_number").setValue(current_user_phone_number);
                    last_msg_ref_2.child("name").setValue(current_user_name);
                    last_msg_ref_2.child("sender").setValue(uid);
                    last_msg_ref_2.child("msg_text").setValue("Photo");
                    last_msg_ref_2.child("msg_time").setValue(getTime());
                    last_msg_ref_2.child("msg_state").setValue("1");
                    last_msg_ref_2.child("msg_image").setValue("true");
                    last_msg_ref_2.child("fcm_id").setValue(current_user_fcm_id);

                    final DatabaseReference unread_msg_ref2 = last_msg_ref_2.child("unread_msg");
                    unread_msg_ref2.keepSynced(true);
                    unread_msg_ref2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if(dataSnapshot.exists()) {
                                if (!String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString())).equals("0")) {
                                    unread_msg_ref2.setValue(String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString()) + 1));
                                } else {
                                    unread_msg_ref2.setValue("1");
                                }
                            }
                            else {
                                unread_msg_ref2.setValue("1");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    chat_ref_1.child(msgSendRef2.getKey()).child("msg_state").setValue("1");
                }
            });

            StorageReference chat_image_storageRef1 = storageRef.child("images/chat_image_data/" + uid + "/" + chat_with_uid + "/IMG_BM_" + String.valueOf(System.currentTimeMillis()) + "_" + file.getLastPathSegment());
            UploadTask uploadTask1 = chat_image_storageRef1.putFile(file);
            uploadTask1.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("upload", "failed");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    downloadUrl = taskSnapshot.getDownloadUrl().toString();
                    msgSendRef2.child("msg_text").setValue(downloadUrl);
                    chat_ref_1.child(msgSendRef2.getKey()).child("msg_text").setValue(downloadUrl);
                }
            });

            StorageReference chat_image_storageRef2 = storageRef.child("images/chat_image_data/" + chat_with_uid + "/" + uid + "/IMG_BM_" + String.valueOf(System.currentTimeMillis()) + "_" + file.getLastPathSegment());
            UploadTask uploadTask2 = chat_image_storageRef2.putFile(file);
            uploadTask2.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("upload", "failed");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    downloadUrl = taskSnapshot.getDownloadUrl().toString();
                    msgSendRef2.child("msg_text").setValue(downloadUrl);
                    chat_ref_2.child(msgSendRef2.getKey()).child("msg_text").setValue(downloadUrl);
                }
            });

        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEmpty(messageArea)) {

                    final String messageText = messageArea.getText().toString();

                    clear();

                    if (settings_prefs.getBoolean("notification_sound", true)) {
                        r.play();
                    }

                    //Current User last message
                    last_msg_ref_1.child("last_modified").setValue(-1 * System.currentTimeMillis());
                    last_msg_ref_1.child("uid").setValue(chat_with_uid);
                    last_msg_ref_1.child("phone_number").setValue(chat_with_phone_number);
                    last_msg_ref_1.child("name").setValue(chat_with_name);
                    last_msg_ref_1.child("sender").setValue(uid);
                    last_msg_ref_1.child("msg_text").setValue(messageText);
                    last_msg_ref_1.child("msg_time").setValue(getTime());
                    last_msg_ref_1.child("msg_state").setValue("1");
                    last_msg_ref_1.child("msg_image").setValue("false");
                    last_msg_ref_1.child("fcm_id").setValue(chat_with_fcm_id);

                    final DatabaseReference unread_msg_ref3 = last_msg_ref_1.child("unread_msg");
                    unread_msg_ref3.setValue("0");
                    unread_msg_ref3.keepSynced(true);

                    /*unread_msg_ref3.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                if (!String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString())).equals("0")) {
                                    unread_msg_ref3.setValue(String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString()) + 1));
                                } else {
                                    unread_msg_ref3.setValue("1");
                                }
                            }
                            else {
                                    unread_msg_ref3.setValue("1");
                                }
                            }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });*/

                    //Chat with user message
                    final Random rn = new Random();
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("msg_uid", System.currentTimeMillis() + rn.nextInt(100));
                    map.put("msg_text", messageText);
                    map.put("msg_image", "false");
                    map.put("msg_time", getTime());
                    map.put("msg_sender", uid);
                    map.put("msg_state", "1");

                    final DatabaseReference msgSendRef = chat_ref_2.push();

                    msgSendRef.setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            msgSendRef.child("msg_state").setValue("1");
                            last_msg_ref_1.child("msg_state").setValue("1");
                        }
                    });

                    //Chat with user last message
                    chat_ref_1.child(msgSendRef.getKey()).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            last_msg_ref_2.child("last_modified").setValue(-1 * System.currentTimeMillis());
                            last_msg_ref_2.child("uid").setValue(uid);
                            last_msg_ref_2.child("phone_number").setValue(current_user_phone_number);
                            last_msg_ref_2.child("name").setValue(current_user_name);
                            last_msg_ref_2.child("sender").setValue(uid);
                            last_msg_ref_2.child("msg_text").setValue(messageText);
                            last_msg_ref_2.child("msg_time").setValue(getTime());
                            last_msg_ref_2.child("msg_state").setValue("1");
                            last_msg_ref_2.child("msg_image").setValue("false");
                            last_msg_ref_2.child("fcm_id").setValue(current_user_fcm_id);

                            final DatabaseReference unread_msg_ref4 = last_msg_ref_2.child("unread_msg");
                            unread_msg_ref4.keepSynced(true);

                            unread_msg_ref4.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.exists()) {
                                        Log.d("xxx_fu",dataSnapshot.getValue().toString());
                                        if (!String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString())).equals("0")) {
                                            unread_msg_ref4.setValue(String.valueOf(Integer.valueOf(dataSnapshot.getValue().toString()) + 1));
                                        } else {
                                            unread_msg_ref4.setValue("1");
                                        }
                                    }
                                    else {
                                        unread_msg_ref4.setValue("1");
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            chat_ref_1.child(msgSendRef.getKey()).child("msg_state").setValue("1");
                        }
                    });

                    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                    String notif_msg_txt = "";
                    if (messageText.length() > 40) {
                        notif_msg_txt = messageText.substring(0, 40);
                    } else {
                        notif_msg_txt = messageText;
                    }

                    String url = "https://baatmessenger.firebaseapp.com/send?cwfcmid=" + chat_with_fcm_id + "&cwname=" + current_user_name + "&cwuid=" + uid + "&myname=" + current_user_name + "&msgbody=" + notif_msg_txt;

                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                        }
                    });
                    queue.add(stringRequest);
                }
            }
        });

        emojiButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //If popup is not showing => emoji keyboard is not visible, we need to show it
                if (!popup.isShowing()) {

                    //If keyboard is visible, hide it, simply show the emoji popup
                    if (popup.isKeyBoardOpen()) {
                        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromInputMethod(messageArea.getWindowToken(), InputMethodManager.SHOW_IMPLICIT);
                        popup.showAtBottom();
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.keyboard_button);
                    }

                    //else, open the text keyboard first and immediately after that show the emoji popup
                    else {
                        messageArea.setFocusableInTouchMode(true);
                        messageArea.requestFocus();
                        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(messageArea, InputMethodManager.SHOW_IMPLICIT);
                        popup.showAtBottomPending();
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.keyboard_button);
                    }
                }

                //If popup is showing, simply dismiss it to show the undelying text keyboard
                else {
                    popup.dismiss();
                    final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(messageArea, InputMethodManager.SHOW_IMPLICIT);
                    changeEmojiKeyboardIcon(emojiButton, R.drawable.emoji_button);
                }
            }
        });

        messageArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (popup.isShowing()) {

                    if (popup.isKeyBoardOpen()) {
                        popup.dismiss();
                        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(messageArea, InputMethodManager.SHOW_IMPLICIT);
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.emoji_button);
                    }
                }
            }
        });

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (popup.isShowing()) {

                    if (popup.isKeyBoardOpen()) {
                        popup.dismiss();
                        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(messageArea, InputMethodManager.SHOW_IMPLICIT);
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.emoji_button);
                    }
                }
            }
        });


        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(Chat.this, new String[]{CAMERA}, 111);
            }
        });

        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, PICK_IMAGE);
            }
        });
    }

    private static final int PICK_IMAGE = 1;
    private static final int PICK_WALLPAPER = 2;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePath = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String picturePath = c.getString(columnIndex);
            c.close();

            Intent intent3 = new Intent(this, PhotoSendGallery.class);
            intent3.putExtra("uid", uid);
            intent3.putExtra("chat_with_uid", chat_with_uid);
            intent3.putExtra("photo_uri", picturePath);
            intent3.putExtra("chat_with_name", chat_with_name);
            intent3.putExtra("chat_with_fcm_id", chat_with_fcm_id);
            intent3.putExtra("chat_with_phone_number", chat_with_phone_number);
            startActivity(intent3);
            finish();
        }

        if (requestCode == PICK_WALLPAPER && resultCode == RESULT_OK && data != null) {

            if (data.hasExtra("set_default_wallpaper")) {
                prefs.edit().putString("wallpaper", "default").apply();
                rootView.setBackgroundColor(Color.parseColor("#E4DCD3"));
            } else {

                Uri selectedImage_intent_uri = data.getData();
                String selectedImage_uri_real = getRealPathFromUri(this, selectedImage_intent_uri);

                File ofile = new File(selectedImage_uri_real);
                File cfile_dir = new File(Environment.getExternalStorageDirectory() + "/BaatMessenger/Media/Wallpaper/");
                if (!cfile_dir.exists()) {
                    if (!cfile_dir.mkdirs()) {
                        Log.d("BaatMessenger", "failed to create directory");
                    }
                }
                // Create a media file name
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File cfile;

                cfile = new File(cfile_dir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

                try {
                    copy(ofile, cfile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (cfile.exists()) {
                    Bitmap bmp1 = BitmapFactory.decodeFile(cfile.getAbsolutePath());
                    Drawable wallpaper = new BitmapDrawable(getResources(), scaleCenterCrop(bmp1, height - 200, width));
                    prefs.edit().putString("wallpaper", String.valueOf(cfile.getAbsolutePath())).apply();
                    rootView.setBackground(wallpaper);

                } else {
                    rootView.setBackgroundColor(Color.parseColor("#E4DCD3"));
                }
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            layout.removeAllViews();
            myConnectionsRef.setValue(Boolean.FALSE);
            mylastOnlineRef.setValue(ServerValue.TIMESTAMP);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        layout.removeAllViews();
        chat_ref_1.addChildEventListener(chat_listener);

        getPresenceStatus();

        final DatabaseReference unread_msg_ref = last_msg_ref_2.child("unread_msg");

        final IntentFilter theFilter = new IntentFilter();

        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);
        theFilter.addAction(Intent.ACTION_USER_PRESENT);

        myConnectionsRef.setValue(Boolean.TRUE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        chat_ref_1.removeEventListener(chat_listener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case 111: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Chat.this, CameraActivity.class);
                    intent.putExtra("uid", uid);
                    intent.putExtra("chat_with_uid", chat_with_uid);
                    intent.putExtra("chat_with_name", chat_with_name);
                    intent.putExtra("chat_with_phone_number", chat_with_phone_number);
                    intent.putExtra("chat_with_fcm_id", chat_with_fcm_id);

                    startActivity(intent);
                    finish();

                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(Chat.this);

                        builder.setTitle("Permission Denied");
                        builder.setIcon(R.mipmap.logo);
                        builder.setCancelable(true);
                        builder.setMessage("Camera permission is required to access camera. To continue Allow the permission from settings.");
                        builder.setPositiveButton("GO TO SETTINGS", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        });
                        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                        positiveButton.setTextColor(Color.parseColor("#FF7B7979"));
                        negativeButton.setTextColor(Color.parseColor("#FF7B7979"));
                    }
                }
            }
        }
    }

    private void getPresenceStatus() {

        chatWithUserPrivacyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String last_seen = dataSnapshot.child("last_seen").getValue().toString();

                    if (settings_prefs.getString("last_seen", "Everyone").equals("Everyone") || settings_prefs.getString("last_seen", "Everyone").equals("My contacts")) {
                        if (last_seen.equals("Everyone") || last_seen.equals("My contacts")) {

                            chatWithUserConnectionsRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        actionbar_user_status.setVisibility(View.VISIBLE);

                                        if (snapshot.getValue().toString().equals("true")) {
                                            actionbar_user_status.setText("online");
                                        } else {
                                            lastOnlineRef.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot last_seen_snapshot) {
                                                    if (last_seen_snapshot.exists()) {
                                                        long now = System.currentTimeMillis();
                                                        long timestamp = Long.parseLong(last_seen_snapshot.getValue().toString());
                                                        actionbar_user_status.setText("last seen " + DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString());
                                                    } else {
                                                        actionbar_user_status.setText("");
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                }
                                            });
                                        }
                                    } else {
                                        actionbar_user_status.setVisibility(View.GONE);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                        else{
                            actionbar_user_status.setVisibility(View.GONE);
                        }
                    }
                    else {
                        actionbar_user_status.setVisibility(View.GONE);
                    }

                } else {
                    actionbar_user_status.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void clear() {
        messageArea.setText("");
    }

    private void changeEmojiKeyboardIcon(ImageView iconToBeChanged, int drawableResourceId) {
        iconToBeChanged.setImageResource(drawableResourceId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_contact:

                Intent intent = new Intent(Chat.this, ViewContact.class);
                intent.putExtra("view_contact_uid", chat_with_uid);
                intent.putExtra("view_contact_name", chat_with_name);
                intent.putExtra("view_contact_phone_number", chat_with_phone_number);
                startActivity(intent);
                return true;

            case R.id.action_media:
                Intent intent2 = new Intent(Chat.this, ChatMediaView.class);
                intent2.putExtra("chat_with_uid", chat_with_uid);
                intent2.putExtra("chat_with_name", chat_with_name);
                startActivity(intent2);
                return true;

            case R.id.action_wallpaper:

                Intent wallpaperpick = new Intent(Intent.ACTION_PICK);
                wallpaperpick.setType("image/*");
                Intent chooser = new Intent(Intent.ACTION_CHOOSER);
                chooser.putExtra(Intent.EXTRA_INTENT, wallpaperpick);
                chooser.putExtra(Intent.EXTRA_TITLE, "Set Wallpaper");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{new Intent(getApplicationContext(), SetDefaultWallpaper.class)});
                startActivityForResult(chooser, PICK_WALLPAPER);
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getTextSize() {
        int textsize = 16;
        String textsize_str = settings_prefs.getString("chat_font_size", "Medium");
        switch (textsize_str) {
            case "Small":
                textsize = 14;
                break;
            case "Medium":
                textsize = 16;
                break;
            case "Large":
                textsize = 18;
                break;
        }
        return textsize;
    }

    private void addImageBox(final String message, final String photo_caption, final String msg_time, final int type, final String msg_state, int text_size, String msg_id, final String msg_uid) {
        LinearLayout chat_view_image = (LinearLayout) getLayoutInflater().inflate(R.layout.chat_message_image, null);
        final ImageView chat_view_image_data = (ImageView) chat_view_image.findViewById(R.id.chat_msg_image);
        EmojiconTextView chat_view_image_caption = (EmojiconTextView) chat_view_image.findViewById(R.id.chat_msg_image_caption);
        final TextView chat_view_image_time_status = (TextView) chat_view_image.findViewById(R.id.chat_msg_image_time_status);

        chat_view_image_data.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (message.equals("default")) {
            Glide.with(getApplicationContext())
                    .load(R.drawable.spin_loader)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(chat_view_image_data);
        }

        chat_view_image_time_status.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        chat_view_image_time_status.setTextColor(getResources().getColor(R.color.msg_time));
        chat_view_image_time_status.setTextSize(10);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.FILL_PARENT);
        params.setMargins(0, 0, 0, dptopix(1));
        if (text_size == 14) {
            chat_view_image_caption.setEmojiconSize(dptopix(20));
        } else if (text_size == 16) {
            chat_view_image_caption.setEmojiconSize(dptopix(22));
        } else if (text_size == 18) {
            chat_view_image_caption.setEmojiconSize(dptopix(24));
        }
        chat_view_image_caption.setTextSize(text_size);


        if (photo_caption.equals("")) {
            chat_view_image_caption.setVisibility(View.GONE);
        }

        if (type == 1) {
            params.gravity = Gravity.RIGHT;
            chat_view_image.setLayoutParams(params);
            chat_view_image.setBackgroundResource(R.drawable.image_rounded_corner_1);
            chat_view_image.setClickable(true);
            chat_view_image.setFocusable(true);
            chat_view_image_caption.setText(photo_caption);
            chat_view_image_caption.setTextColor(Color.parseColor("#ffffff"));

            chat_view_image_time_status.setText(msg_time + " ");
            chat_view_image_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, getMsgTicks(msg_state), 0);

            final DatabaseReference msg_ref_text = database.getReference("messages/" + uid + "/" + chat_with_uid + "/" + msg_id + "/msg_text");
            msg_ref_text.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        final String ms_text = dataSnapshot.getValue().toString();
                        if (!ms_text.equals("default")) {

                            chat_view_image_data.post(new Runnable() {
                                public void run() {
                                    chat_view_image_data.invalidate();
                                    Glide.with(getApplicationContext())
                                            .load(ms_text)
                                            .thumbnail(Glide.with(getApplicationContext()).load(R.drawable.spin_loader))
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(chat_view_image_data);

                                    chat_view_image_data.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent2 = new Intent(Chat.this, ViewPhoto.class);
                                            intent2.putExtra("photo_uri", ms_text);
                                            intent2.putExtra("photo_caption", photo_caption);
                                            intent2.putExtra("photo_time", msg_time);
                                            intent2.putExtra("chat_media", "true");
                                            intent2.putExtra("chat_media_sender", "You");
                                            startActivity(intent2);
                                        }
                                    });
                                }
                            });
                            msg_ref_text.removeEventListener(this);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            final DatabaseReference msg_ref_state = database.getReference("messages/" + uid + "/" + chat_with_uid + "/" + msg_id + "/msg_state");
            msg_ref_state.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        final String ms = dataSnapshot.getValue().toString();
                        if (ms.equals("1")) {
                            chat_view_image_time_status.post(new Runnable() {
                                public void run() {
                                    chat_view_image_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.msg_sent, 0);
                                    chat_view_image_time_status.invalidate();
                                }
                            });
                        }
                        if (ms.equals("2")) {
                            chat_view_image_time_status.post(new Runnable() {
                                public void run() {
                                    //text.invalidate();
                                    chat_view_image_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.msg_delivered, 0);
                                    chat_view_image_time_status.invalidate();
                                }
                            });
                        }
                        if (ms.equals("3")) {
                            chat_view_image_time_status.post(new Runnable() {
                                public void run() {
                                    //text.invalidate();
                                    chat_view_image_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.msg_read, 0);
                                    chat_view_image_time_status.invalidate();
                                }
                            });
                            msg_ref_state.removeEventListener(this);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            params.gravity = Gravity.LEFT;
            chat_view_image.setLayoutParams(params);
            chat_view_image.setBackgroundResource(R.drawable.image_rounded_corner_2);
            chat_view_image.setClickable(true);
            chat_view_image.setFocusable(true);
            chat_view_image_caption.setText(photo_caption);
            chat_view_image_caption.setTextColor(Color.parseColor("#000000"));

            final DatabaseReference msg_ref_text = database.getReference("messages/" + uid + "_" + chat_with_uid + "/" + msg_id + "/msg_text");
            msg_ref_text.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        final String ms_text = dataSnapshot.getValue().toString();
                        if (!ms_text.equals("default")) {

                            chat_view_image_data.post(new Runnable() {
                                public void run() {
                                    chat_view_image_data.invalidate();
                                    Glide.with(getApplicationContext())
                                            .load(ms_text)
                                            .thumbnail(Glide.with(getApplicationContext()).load(R.drawable.spin_loader))
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(chat_view_image_data);

                                    chat_view_image_data.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent2 = new Intent(Chat.this, ViewPhoto.class);
                                            intent2.putExtra("photo_uri", ms_text);
                                            intent2.putExtra("photo_caption", photo_caption);
                                            intent2.putExtra("photo_time", msg_time);
                                            intent2.putExtra("chat_media", "true");
                                            intent2.putExtra("chat_media_sender", "You");
                                            startActivity(intent2);
                                        }
                                    });
                                }
                            });
                            msg_ref_text.removeEventListener(this);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            chat_view_image_time_status.setText(msg_time);
        }

        layout.addView(chat_view_image);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void addMessageBox(final String message, final String message_time, int type, final String msg_state, int text_size, String msg_id, final String msg_uid) {

        LinearLayout chat_view = (LinearLayout) getLayoutInflater().inflate(R.layout.chat_message, null);
        chat_view.setMinimumWidth(dptopix(85));

        EmojiconTextView chat_msg_text = (EmojiconTextView) chat_view.findViewById(R.id.chat_msg_text);
        chat_msg_text.setMaxWidth(dptopix(270));
        final TextView chat_msg_time_status = (TextView) chat_view.findViewById(R.id.chat_msg_time_status);
        chat_msg_time_status.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        chat_msg_time_status.setTextColor(getResources().getColor(R.color.msg_time));
        chat_msg_time_status.setTextSize(10);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.FILL_PARENT);
        params.setMargins(0, 0, 0, dptopix(1));
        if (text_size == 14) {
            chat_msg_text.setEmojiconSize(dptopix(20));
        } else if (text_size == 16) {
            chat_msg_text.setEmojiconSize(dptopix(22));
        } else if (text_size == 18) {
            chat_msg_text.setEmojiconSize(dptopix(24));
        }
        chat_msg_text.setTextSize(text_size);

        if (type == 1) {
            params.gravity = Gravity.RIGHT;
            chat_view.setLayoutParams(params);
            chat_view.setBackgroundResource(R.drawable.chat_box_1);
            chat_view.setClickable(true);
            chat_view.setFocusable(true);
            chat_msg_text.setText(message);
            chat_msg_text.setTextColor(Color.parseColor("#ffffff"));

            chat_msg_time_status.setText(message_time + " ");
            chat_msg_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, getMsgTicks(msg_state), 0);

            final DatabaseReference msg_ref = database.getReference("messages/" + uid + "/" + chat_with_uid + "/" + msg_id + "/msg_state");
            msg_ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        final String ms = dataSnapshot.getValue().toString();
                        if (ms.equals("1")) {
                            chat_msg_time_status.post(new Runnable() {
                                public void run() {
                                    chat_msg_time_status.setText(message_time + " ");
                                    chat_msg_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.msg_sent, 0);
                                    chat_msg_time_status.invalidate();
                                }
                            });
                        }
                        if (ms.equals("2")) {
                            chat_msg_time_status.post(new Runnable() {
                                public void run() {
                                    chat_msg_time_status.setText(message_time + " ");
                                    chat_msg_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.msg_delivered, 0);
                                    chat_msg_time_status.invalidate();
                                }
                            });
                        }
                        if (ms.equals("3")) {
                            chat_msg_time_status.post(new Runnable() {
                                public void run() {
                                    chat_msg_time_status.setText(message_time + " ");
                                    chat_msg_time_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.msg_read, 0);
                                    chat_msg_time_status.invalidate();
                                }
                            });
                            msg_ref.removeEventListener(this);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        } else {
            params.gravity = Gravity.LEFT;
            chat_view.setLayoutParams(params);
            chat_view.setBackgroundResource(R.drawable.chat_box_2);
            chat_view.setClickable(true);
            chat_view.setFocusable(true);
            chat_msg_text.setText(message);
            chat_msg_text.setTextColor(Color.parseColor("#000000"));

            chat_msg_time_status.setText(message_time);
        }

        layout.addView(chat_view);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private int getMsgTicks(String msg_state) {
        int return_ticks = 1;
        switch (msg_state) {
            case "1":
                return_ticks = R.drawable.msg_sent;
                break;
            case "2":
                return_ticks = R.drawable.msg_delivered;
                break;
            case "3":
                return_ticks = R.drawable.msg_read;
                break;
        }
        return return_ticks;
    }

    protected static String getRealPathFromUri(Activity activity, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    protected static void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    private static Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    private int dptopix(int pix) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pix, getResources().getDisplayMetrics());
    }

    private boolean isEmpty(EmojiconEditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }
}