package saurav.chandra.baatmessenger;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL;

public class ChatMediaAdapter extends ArrayAdapter<ChatMedia> {

    ChatMediaAdapter(Context context, ArrayList<ChatMedia> chatm) {
        super(context, 0, chatm);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final ChatMedia chat_m = getItem(position);

        View gridView;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            gridView = inflater.inflate(R.layout.chat_media_item, null);

            ImageView imageView = (ImageView) gridView.findViewById(R.id.chat_media_item);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(getContext())
                    .load(chat_m.chat_media_uri)
                    .thumbnail(Glide.with(getContext()).load(R.drawable.image_loader))
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), ViewPhoto.class);

                    intent.putExtra("chat_media", "true");
                    intent.putExtra("chat_media_sender", chat_m.chat_media_sender);
                    intent.putExtra("photo_uri", chat_m.chat_media_uri);
                    intent.putExtra("photo_caption", chat_m.chat_media_caption);
                    intent.putExtra("photo_time", chat_m.chat_media_time);
                    getContext().startActivity(intent);
                }
            });

        }
        else {
            gridView = convertView;
        }
        return gridView;
    }
}