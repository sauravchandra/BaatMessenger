package saurav.chandra.baatmessenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SetDefaultWallpaper extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent resultIntent = new Intent();
        resultIntent.putExtra("set_default_wallpaper", true);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}