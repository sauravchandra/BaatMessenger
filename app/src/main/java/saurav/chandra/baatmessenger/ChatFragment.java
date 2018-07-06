package saurav.chandra.baatmessenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static saurav.chandra.baatmessenger.MainActivity.phone_number;
import static saurav.chandra.baatmessenger.MainActivity.uid;

public class ChatFragment extends Fragment {

    private ChatAdapter chat_adapter;
    private ListView chatlist;
    private RelativeLayout empty_chatlist,contacts_permission;
    private ProgressDialog mProgressDialog;

    private static ArrayList<String> ChatUsers = new ArrayList<String>();
    private static ArrayList<ChatL> Chats = new ArrayList<ChatL>();

    private FirebaseDatabase database;
    private DatabaseReference last_msg_ref;
    private Query query;
    private Button permission_button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        database = ((BaatMessenger) getActivity().getApplication()).getDatabase();
        last_msg_ref = database.getReference("lastMessage");
        query = last_msg_ref.orderByChild("last_modified");
        query.keepSynced(true);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        chatlist = (ListView) getActivity().findViewById(R.id.chatList);
        empty_chatlist = (RelativeLayout) getActivity().findViewById(R.id.emptyChatList);
        contacts_permission = (RelativeLayout) getActivity().findViewById(R.id.noContactsPermission_chat);
        permission_button = (Button) getActivity().findViewById(R.id.allow_permission_chat);
        permission_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                requestPermissions(new String[]{READ_CONTACTS} , 11);
            }
        });

        chatlist.setVisibility(View.GONE);
        empty_chatlist.setVisibility(View.VISIBLE);
        contacts_permission.setVisibility(View.GONE);

        if (!Functions.checkPermission(new String[]{READ_CONTACTS}, getActivity())) {
            getContacts();
            getChatDataFromServer();
        }
        else {
            chatlist.setVisibility(View.GONE);
            empty_chatlist.setVisibility(View.GONE);
            contacts_permission.setVisibility(View.VISIBLE);
        }

        chat_adapter = new ChatAdapter(getActivity().getApplicationContext(), Chats);
        chatlist.setAdapter(chat_adapter);
    }

    private void getContacts(){
        Cursor phones = getActivity().getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);

        while (phones.moveToNext())
        {
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            ChatUsers.add(phoneNumberFormat(phoneNumber));
        }
        phones.close();
    }

    private String phoneNumberFormat(String phonenumber){
        phonenumber=phonenumber.replaceAll("\\s+","");
        phonenumber=phonenumber.replace("-","");
        if(phonenumber.substring(0,1).equals("0")){phonenumber=phonenumber.substring(1);}
        if(phonenumber.length()==10){phonenumber="+91"+phonenumber;}

        return phonenumber;
    }

    private void getChatDataFromServer(){
        showProgressDialog();
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot lastmsgdataSnapshot) {
                if (lastmsgdataSnapshot.exists()) {//lastMessage
                    Chats.clear();
                    for (DataSnapshot last_msg_data_snapshot : lastmsgdataSnapshot.getChildren()) {

                        if (last_msg_data_snapshot.hasChildren()) {
                            final String chat_uids = last_msg_data_snapshot.getKey();
                            if (chat_uids.contains(uid + "_")) {

                                final String lastm_uid = (String) last_msg_data_snapshot.child("uid").getValue();
                                final String lastm_phone_number = (String) last_msg_data_snapshot.child("phone_number").getValue();
                                final String lastm_message = (String) last_msg_data_snapshot.child("msg_text").getValue();
                                final String lastm_msg_state = (String) last_msg_data_snapshot.child("msg_state").getValue();
                                final String lastm_msg_sender = (String) last_msg_data_snapshot.child("sender").getValue();
                                String lastm_name = (String) last_msg_data_snapshot.child("name").getValue();

                                if (!(ChatUsers.contains(lastm_phone_number)) && !(Objects.equals(lastm_phone_number, phone_number))) {
                                    lastm_name = lastm_phone_number;
                                }

                                final String lastm_message_time = (String) last_msg_data_snapshot.child("msg_time").getValue();
                                final String lastm_fcm_id = (String) last_msg_data_snapshot.child("fcm_id").getValue();
                                String lastm_unread_msg = (String) last_msg_data_snapshot.child("unread_msg").getValue();

                                Chats.add(new ChatL(lastm_uid, lastm_phone_number, lastm_name, lastm_message, lastm_msg_sender, lastm_msg_state, lastm_message_time, lastm_fcm_id,lastm_unread_msg));
                                chat_adapter.notifyDataSetChanged();
                                chatlist.setVisibility(View.VISIBLE);
                                empty_chatlist.setVisibility(View.GONE);
                                contacts_permission.setVisibility(View.GONE);
                            }
                        }
                    }
                }
                else {
                    chatlist.setVisibility(View.GONE);
                    empty_chatlist.setVisibility(View.VISIBLE);
                    contacts_permission.setVisibility(View.GONE);
                }
                hideProgressDialog();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Failed to read value.", "error database");
                hideProgressDialog();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 11: {

                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    getContacts();
                    getChatDataFromServer();
                }
                else{
                    if (!shouldShowRequestPermissionRationale(READ_CONTACTS)) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                        builder.setTitle("Permission Denied");
                        builder.setIcon(R.mipmap.logo);
                        builder.setCancelable(true);
                        builder.setMessage("Contacts permission is required for the app to function. To continue Allow the permission from settings.");
                        builder.setPositiveButton("GO TO SETTINGS", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        });
                        builder.setNegativeButton("CANCEL",  new DialogInterface.OnClickListener() {
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

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getContext());
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private String getMessageTime(String timestamp){
        return "maakabhosda";
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}