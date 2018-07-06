package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static saurav.chandra.baatmessenger.Chat.copy;
import static saurav.chandra.baatmessenger.Chat.getRealPathFromUri;

public class UserDet_Chat extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
    TextView actionbar_activity_name;

    private static FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;

    private SharedPreferences prefs;

    private FirebaseStorage storage;
    private StorageReference chatMediaRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_userdetails, null);
        actionbar_activity_name = (TextView) actionBarLayout.findViewById(R.id.actionbar_activity_name);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        final String uid = currentUser.getUid();

        prefs = getSharedPreferences(Config.SHARED_PREF, MODE_PRIVATE);

        database = ((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/" + uid + "/connected");
        lastOnlineRef = database.getReference("presenceStatus/" + uid + "/lastOnline");

        storage = ((BaatMessenger) this.getApplication()).getStorage();
        chatMediaRef = storage.getReference().child("images/chat_image_data");

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back_white_normal);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF710C")));

        actionbar_activity_name.setText("Chat");
        addPreferencesFromResource(R.xml.chat_pref);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Preference chatFontSizePref = findPreference("chat_font_size");
        chatFontSizePref.setSummary(preferences.getString("chat_font_size", ""));
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        Preference delChatPref = findPreference("delete_chat");
        delChatPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final AlertDialog dialog = new AlertDialog.Builder(preference.getContext())
                        .setTitle("Delete Chats")
                        .setMessage("Do you want to delete all chats?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                ProgressDialog mProgressDialog = new ProgressDialog(getApplicationContext());
                                mProgressDialog.setMessage("Deleting Chats...");
                                mProgressDialog.setIndeterminate(true);

                                DatabaseReference delChatRef_messages = database.getReference("messages");
                                delChatRef_messages.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot delChatMessageSnapshot) {

                                        for (DataSnapshot delc_snapshot : delChatMessageSnapshot.getChildren()) {
                                            String uids = delc_snapshot.getKey();

                                            if (uids.contains(uid + "_")) {
                                                delc_snapshot.getRef().removeValue();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                                DatabaseReference delChatRef_lastMessages = database.getReference("lastMessage");
                                delChatRef_lastMessages.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot delChatMessageSnapshot) {
                                        for (DataSnapshot delm_snapshot : delChatMessageSnapshot.getChildren()) {
                                            String uids = delm_snapshot.getKey();

                                            if (uids.contains(uid + "_")) {
                                                delm_snapshot.getRef().removeValue();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                mProgressDialog.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }).create();

                dialog.show();
                return true;
            }

        });

        Preference wallpaperPref = findPreference("wallpaper");
        wallpaperPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent wallpaperpick = new Intent(Intent.ACTION_PICK);
                wallpaperpick.setType("image/*");
                Intent chooser = new Intent(Intent.ACTION_CHOOSER);
                chooser.putExtra(Intent.EXTRA_INTENT, wallpaperpick );
                chooser.putExtra(Intent.EXTRA_TITLE, "Set Wallpaper");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{ new Intent(getApplicationContext(),SetDefaultWallpaper.class)} );
                startActivityForResult(chooser,123);
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 123 && resultCode == RESULT_OK && data !=null) {

            if (data.hasExtra("set_default_wallpaper")) {
                prefs.edit().putString("wallpaper", "default").apply();
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
                    prefs.edit().putString("wallpaper", String.valueOf(cfile.getAbsolutePath())).apply();
                } else {
                    prefs.edit().putString("wallpaper", "default").apply();
                }
            }
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("chat_font_size")) {
            Preference chatFontSizePref = findPreference(key);
            chatFontSizePref.setSummary(sharedPreferences.getString(key, ""));
        }
    }
}
