package saurav.chandra.baatmessenger;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import static saurav.chandra.baatmessenger.MainActivity.currentUser;
import static saurav.chandra.baatmessenger.MainActivity.uid;

public class ChatAdapter extends ArrayAdapter<ChatL> {

    private FirebaseDatabase database;

    ChatAdapter(Context context, ArrayList<ChatL> chats) {
        super(context, 0, chats);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        final ChatL chat = getItem(position);

        database = ((BaatMessenger) getContext()).getDatabase();

        // Check if an existing view is being reused, otherwise inflate the view
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.chatlist_row, parent, false);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getContext(), Chat.class);
                            intent.putExtra("chat_with_uid", chat.user_uid);
                            intent.putExtra("chat_with_name", chat.user_name);
                            intent.putExtra("chat_with_fcm_id", chat.user_chat_with_fcm_id);
                            intent.putExtra("chat_with_phone_number", chat.user_phonenumber);

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getContext().startActivity(intent);
                        }
                    });
                }

                FirebaseStorage storage = ((BaatMessenger) this.getContext()).getStorage();
                StorageReference storageRef = storage.getReference();

                //Get chat with user profile picture
                StorageReference profilePicRef = storageRef.child("images/profilepic/" + chat.user_uid);


                // Lookup view for data population
                ImageView ivPhoto = (ImageView) convertView.findViewById(R.id.imageView);
                TextView tvName = (TextView) convertView.findViewById(R.id.textViewNameChat);
                final EmojiconTextView tvMsg = (EmojiconTextView) convertView.findViewById(R.id.textViewMsg);
                TextView tvTime = (TextView) convertView.findViewById(R.id.textViewMsgTime);
                TextView tvUnreadMsg = (TextView) convertView.findViewById(R.id.textViewUnreadMsg);

                // Populate the data into the template view using the data object

                Glide.with(getContext())                             //Set chat with user photo
                        .using(new FirebaseImageLoader())
                        .load(profilePicRef)
                        .crossFade()
                        .thumbnail(Glide.with(getContext()).load(R.drawable.default_dp).transform(new CircleTransform(getContext())))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transform(new CircleTransform(getContext()))
                        .into(ivPhoto);

                ivPhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent2 = new Intent(getContext(), ViewPhoto.class);
                        intent2.putExtra("profile_pic", "true");
                        intent2.putExtra("chat_with_uid", chat.user_uid);
                        intent2.putExtra("chat_with_name", chat.user_name);
                        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent2);
                    }
                });

                String msg_text = chat.user_last_message.length() < 39 ? chat.user_last_message : chat.user_last_message.substring(0, 38) + "...";

                if (chat.user_last_message_sender.equals(MainActivity.uid)) {
                    tvUnreadMsg.setVisibility(View.GONE);
                    tvMsg.setTypeface(null, Typeface.NORMAL);
                    tvTime.setTypeface(null, Typeface.NORMAL);
                    tvName.setTextColor(Color.parseColor("#000000"));
                    tvMsg.setTextColor(Color.parseColor("#616161"));
                    tvTime.setTextColor(Color.parseColor("#616161"));
                    tvName.setText(chat.user_name);
                    tvMsg.setCompoundDrawablesWithIntrinsicBounds(getMsgTicks(chat.user_last_message_state), 0,0 , 0);

                    final DatabaseReference msg_ref = database.getReference("lastMessage/" + uid+"_"+chat.user_uid+"/msg_state");
                    msg_ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                final String ms = dataSnapshot.getValue().toString();
                                if (ms.equals("1")) {
                                    tvMsg.post(new Runnable() {
                                        public void run() {
                                            tvMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.msg_sent_dark, 0,0 , 0);
                                            tvMsg.invalidate();
                                        }
                                    });
                                }
                                if (ms.equals("2")) {
                                    tvMsg.post(new Runnable() {
                                        public void run() {
                                            tvMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.msg_delivered_dark, 0,0 , 0);
                                            tvMsg.invalidate();
                                        }
                                    });
                                }
                                if (ms.equals("3")) {
                                    tvMsg.post(new Runnable() {
                                        public void run() {
                                            tvMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.msg_read, 0,0 , 0);
                                            tvMsg.invalidate();
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

                    tvMsg.setText(" "+msg_text);
                    if(msg_text.equals("Photo")){
                        tvMsg.setText(" "+msg_text);
                        tvMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.photo_button, 0, 0, 0);
                    }
                    tvTime.setText(chat.user_last_message_time);
                } else {
                    if (chat.user_last_message_state.equals("1")) {
                        if(!chat.user_unread_msg.equals("0")){
                            tvUnreadMsg.setVisibility(View.VISIBLE);
                            tvUnreadMsg.setText(chat.user_unread_msg);
                        }
                        else{
                            tvUnreadMsg.setVisibility(View.GONE);
                        }
                        tvMsg.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0);
                        tvMsg.setTypeface(null, Typeface.BOLD);
                        tvTime.setTypeface(null, Typeface.BOLD);
                        tvName.setTextColor(Color.parseColor("#000000"));
                        tvMsg.setTextColor(Color.parseColor("#FF710C"));
                        tvTime.setTextColor(Color.parseColor("#FF710C"));
                        tvName.setText(chat.user_name);
                        tvMsg.setText(" "+msg_text);
                        if(msg_text.equals("Photo")){
                            tvMsg.setText(" "+msg_text);
                            tvMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.photo_button_colored, 0, 0, 0);
                        }
                        tvTime.setText(chat.user_last_message_time);
                    } else {
                        tvUnreadMsg.setVisibility(View.GONE);
                        tvMsg.setTypeface(null, Typeface.NORMAL);
                        tvTime.setTypeface(null, Typeface.NORMAL);
                        tvName.setTextColor(Color.parseColor("#000000"));
                        tvMsg.setTextColor(Color.parseColor("#616161"));
                        tvTime.setTextColor(Color.parseColor("#616161"));
                        tvName.setText(chat.user_name);
                        tvMsg.setText(msg_text);
                        if(msg_text.equals("Photo")){
                            tvMsg.setText(" "+msg_text);
                            tvMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.photo_button, 0, 0, 0);
                        }
                        tvTime.setText(chat.user_last_message_time);
                    }
                }
            return convertView;
    }

    private int getMsgTicks(String msg_state){
        int return_ticks = 1;
        switch (msg_state) {
            case "1":
                return_ticks = R.drawable.msg_sent;break;
            case "2":
                return_ticks = R.drawable.msg_delivered;break;
            case "3":
                return_ticks = R.drawable.msg_read;break;
        }
        return return_ticks;
    }
}