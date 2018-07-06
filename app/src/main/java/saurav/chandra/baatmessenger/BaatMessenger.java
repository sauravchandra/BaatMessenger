package saurav.chandra.baatmessenger;

import android.app.Application;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import io.fabric.sdk.android.Fabric;

public class BaatMessenger extends Application {

    private FirebaseDatabase database;
    private FirebaseStorage storage;

    @Override
    public void onCreate() {

        super.onCreate();
        Fabric.with(this, new Crashlytics());
        setDatabase();
        setStorage();
    }

    public void setDatabase(){
        database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }

    public FirebaseStorage getStorage() {
        return storage;
    }

    public void setStorage(){
        storage = FirebaseStorage.getInstance();
    }

}


