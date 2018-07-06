package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class DeleteAccount extends Activity {

    TextView actionbar_activity_name;

    private static FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference delDataRef,delMsgRef,delLastMsgRef,delTypingRef,delPresenceRef;
    private String uid;
    private LinearLayout acc_del_layout,acc_del_progress_layout;
    private FirebaseStorage storage;
    private StorageReference storageRef,profilePicRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        setContentView(R.layout.activity_delete_account);

        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_actionbar_userdetails, null);
        actionbar_activity_name = (TextView) actionBarLayout.findViewById(R.id.actionbar_activity_name);

        acc_del_layout = (LinearLayout) findViewById(R.id.acc_del_layout);
        acc_del_progress_layout = (LinearLayout) findViewById(R.id.acc_del_progress);
        acc_del_layout.setVisibility(View.VISIBLE);
        acc_del_progress_layout.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        uid = currentUser.getUid();

        database = ((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/" + uid + "/connected");
        lastOnlineRef = database.getReference("presenceStatus/" + uid + "/lastOnline");

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setCustomView(actionBarLayout);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back_white);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF710C")));
        actionbar_activity_name.setText("Delete Account");
    }

    private void deleteAccount(View view) {
        delDataRef = database.getReference("users/" + uid);
        delMsgRef = database.getReference("messages");
        delLastMsgRef = database.getReference("lastMessage");
        delTypingRef = database.getReference("typingStatus");
        delPresenceRef = database.getReference("presenceStatus/"+uid);

        storage = ((BaatMessenger) this.getApplicationContext()).getStorage();
        storageRef = storage.getReference();

        EditText country_code = (EditText) findViewById(R.id.del_acc_country_code);
        final EditText phone_number = (EditText) findViewById(R.id.del_acc_phone_number);

        if (!(country_code.getText().toString().isEmpty()) && !(phone_number.getText().toString().isEmpty())) {

            acc_del_layout.setVisibility(View.GONE);
            acc_del_progress_layout.setVisibility(View.VISIBLE);
            final String number = "+" + country_code.getText().toString() + phone_number.getText().toString();

            delDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot delAccountDataSnapshot) {
                    String uid_phone_number = (String) delAccountDataSnapshot.child("phone_number").getValue();

                    if (number.equals(uid_phone_number)) {
                        delDataRef.removeValue();
                    }

                    delMsgRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot delMessageSnapshot) {
                            for (DataSnapshot del_msg_snapshot : delMessageSnapshot.getChildren()) {
                                final String chat_uids = del_msg_snapshot.getKey();
                                if (chat_uids.contains(uid)) {
                                    del_msg_snapshot.getRef().removeValue();
                                }
                            }

                            delLastMsgRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot delLastMessageSnapshot) {
                                    for (DataSnapshot del_last_msg_snapshot : delLastMessageSnapshot.getChildren()) {
                                        final String chat_uids = del_last_msg_snapshot.getKey();
                                        if (chat_uids.contains(uid)) {
                                            del_last_msg_snapshot.getRef().removeValue();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    delPresenceRef.removeValue();

                    delTypingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot delTypingSnapshot) {

                            for (DataSnapshot del_typing_snapshot : delTypingSnapshot.getChildren()) {
                                final String typing_uids = del_typing_snapshot.getKey();
                                if (typing_uids.contains(uid)) {
                                    del_typing_snapshot.getRef().removeValue();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    profilePicRef = storageRef.child("images/profilepic_high_res/" + uid);
                    profilePicRef.delete();
                    profilePicRef = storageRef.child("images/profilepic/" + uid);
                    profilePicRef.delete();
                    Intent intent = new Intent(getApplicationContext(), WelcomeScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
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
