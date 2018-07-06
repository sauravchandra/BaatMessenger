package saurav.chandra.baatmessenger;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import static saurav.chandra.baatmessenger.ContactsFragment.Users;

public class UsersAdapter extends ArrayAdapter<User> {

    UsersAdapter(Context context, ArrayList<User> users) {
        super(context,0,users);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        final User user = getItem(position);

        if (user != null) {
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.userlist_row, parent, false);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getContext(), Chat.class);
                        intent.putExtra("chat_with_name", user.user_name);
                        intent.putExtra("chat_with_uid", user.user_uid);
                        intent.putExtra("chat_with_fcm_id", user.user_chat_with_fcm_id);
                        intent.putExtra("chat_with_phone_number", user.user_phone);

                        Users.clear();
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent);
                    }
                });

            }

            FirebaseStorage storage = ((BaatMessenger) this.getContext()).getStorage();
            StorageReference storageRef = storage.getReference();

            //Get chat with user profile picture
            StorageReference profilePicRef = storageRef.child("images/profilepic/" + user.user_uid);
            // Lookup view for data population
            ImageView ivPhoto = (ImageView) convertView.findViewById(R.id.imageView);
            TextView tvName = (TextView) convertView.findViewById(R.id.textViewNameUser);

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
                    intent2.putExtra("chat_with_uid", user.user_uid);
                    intent2.putExtra("chat_with_name", user.user_name);
                    intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent2);
                }
            });

            tvName.setText(user.user_name);

        }
        return convertView;
    }
}
