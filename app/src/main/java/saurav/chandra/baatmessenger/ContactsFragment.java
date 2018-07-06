package saurav.chandra.baatmessenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Queue;

import static android.Manifest.permission.READ_CONTACTS;
import static saurav.chandra.baatmessenger.MainActivity.uid;

public class ContactsFragment extends Fragment {

    private UsersAdapter user_adapter;
    private ListView userlist;
    private RelativeLayout empty_userlist,contacts_permission;
    private ProgressDialog mProgressDialog;

    protected static ArrayList<String> ContactUsers = new ArrayList<String>();
    protected static ArrayList<User> Users = new ArrayList<User>();

    private FirebaseDatabase database;
    private DatabaseReference existing_users_ref;
    private Query query;
    private Button permission_button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        database = ((BaatMessenger) getActivity().getApplication()).getDatabase();
        existing_users_ref = database.getReference("users");
        query = existing_users_ref.orderByChild("name");
        query.keepSynced(true);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        userlist = (ListView) getActivity().findViewById(R.id.usersList);
        empty_userlist = (RelativeLayout) getActivity().findViewById(R.id.emptyUserList);
        contacts_permission = (RelativeLayout) getActivity().findViewById(R.id.noContactsPermission_contacts);
        permission_button = (Button) getActivity().findViewById(R.id.allow_permission_contacts);
        permission_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{READ_CONTACTS} , 12);
            }
        });

        userlist.setVisibility(View.GONE);
        empty_userlist.setVisibility(View.VISIBLE);
        contacts_permission.setVisibility(View.GONE);

        if (!Functions.checkPermission(new String[]{READ_CONTACTS}, getActivity())) {
            getContacts();
            getExistingUserDataFromServer();
        }
        else {
            userlist.setVisibility(View.GONE);
            empty_userlist.setVisibility(View.GONE);
            contacts_permission.setVisibility(View.VISIBLE);
        }

        user_adapter = new UsersAdapter(getActivity().getApplicationContext(), Users);
        userlist.setAdapter(user_adapter);
    }

    private void getExistingUserDataFromServer()
    {
        showProgressDialog();
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot existingUserDataSnapshot) {
                if (existingUserDataSnapshot.exists()) {//users
                    Users.clear();

                    for (DataSnapshot user_snapshot : existingUserDataSnapshot.getChildren()) {
                        final String user_uid = user_snapshot.getKey();

                        if (!user_uid.equals(uid)) {
                            final String phone_number = (String) user_snapshot.child("phone_number").getValue();
                            if(ContactUsers.contains(phone_number)){
                                final String name = (String) user_snapshot.child("name").getValue();
                                final String fcm_id = (String) user_snapshot.child("fcm_reg_token").getValue();
                                Users.add(new User(user_uid, name, phone_number, fcm_id));

                                user_adapter.notifyDataSetChanged();
                                userlist.setVisibility(View.VISIBLE);
                                empty_userlist.setVisibility(View.GONE);
                                contacts_permission.setVisibility(View.GONE);
                            }
                        }
                    }
                }
                else {
                    userlist.setVisibility(View.GONE);
                    empty_userlist.setVisibility(View.VISIBLE);
                    contacts_permission.setVisibility(View.GONE);
                }
                hideProgressDialog();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                hideProgressDialog();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 12: {
                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    getContacts();
                    getExistingUserDataFromServer();
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

    private void getContacts(){
        Cursor phones = getActivity().getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);

        while (phones.moveToNext())
        {
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            ContactUsers.add(phoneNumberFormat(phoneNumber));
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

    @Override
    public void onStop() {
        super.onStop();
    }
}
