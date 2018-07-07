package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import id.zelory.compressor.Compressor;
import mbanje.kurt.fabbutton.FabButton;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.support.v4.content.PermissionChecker.checkSelfPermission;
import static saurav.chandra.baatmessenger.Functions.getclip;

public class SetupProfile extends Activity {

    private Button display_pic_select;
    private EditText user_name_profile;
    private byte[] byteArray;

    private FirebaseUser currentUser;

    private File compressedImageFile;
    private static boolean name_data_set,photo_data_set;

    private FirebaseDatabase database;
    private DatabaseReference privacyRef;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference profilePicRef,profilePicRef_high_res;

    private static Uri fileUri;
    private String mCurrentPhotoPath;

    private SharedPreferences prefs;

    private static final int CAMERA_REQUEST_CODE  = 11;
    private static final int STORAGE_REQUEST_CODE  = 22;

    private static final int CAMERA_PERMISSION  = 111;
    private static final int STORAGE_PERMISSION  = 222;
    private static final int MAIN_PERMISSION  = 333;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        setContentView(R.layout.activity_setup_profile);

        ActionBar actionbar = getActionBar();
        actionbar.hide();

        database = ((BaatMessenger) this.getApplication()).getDatabase();
        storage = ((BaatMessenger) this.getApplication()).getStorage();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        prefs = getSharedPreferences(Config.SHARED_PREF, MODE_PRIVATE);

        name_data_set = false;
        photo_data_set = false;

        display_pic_select = (Button) findViewById(R.id.display_pic_select);
        display_pic_select.setText("Set Photo");
        display_pic_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        user_name_profile = (EditText) findViewById(R.id.user_name_profile);

        final FabButton setup_profile_button = (FabButton) findViewById(R.id.setup_profile_button);
        final ProgressHelper helper = new ProgressHelper(setup_profile_button,this);

        setup_profile_button.setVisibility(View.GONE);

        user_name_profile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(user_name_profile.getText().toString().trim().length() != 0 && user_name_profile.getText().toString().trim().length() > 3 ){
                    name_data_set = true;
                    if(photo_data_set) {
                        setup_profile_button.setVisibility(View.VISIBLE);
                    }
                }

                if(user_name_profile.getText().toString().trim().length() < 3 ){
                    name_data_set = false;

                    setup_profile_button.setVisibility(View.GONE);
                }
            }
        });

        setup_profile_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(photo_data_set && name_data_set){

                    if (v != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    helper.startIndeterminate();

                    user_name_profile = (EditText) findViewById(R.id.user_name_profile);

                    final DatabaseReference user_ref = database.getReference("users");
                    final DatabaseReference user_data_ref = database.getReference("usersData");

                    final String uid = currentUser.getUid();

                    storageRef = storage.getReference();
                    profilePicRef = storageRef.child("images/profilepic/" + uid);
                    profilePicRef_high_res = storageRef.child("images/profilepic_high_res/" + uid);
                    privacyRef = database.getReference("privacyStatus/"+uid);

                    profilePicRef.putBytes(byteArray).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            profilePicRef_high_res.putFile(Uri.fromFile(compressedImageFile)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(user_name_profile.getText().toString()).build();
                                    privacyRef.child("last_seen").setValue("Everyone");
                                    privacyRef.child("profile_pic").setValue("Everyone");
                                    currentUser.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (!Functions.checkPermission(new String[]{READ_CONTACTS}, SetupProfile.this)) {
                                                final Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

                                                while (phones.moveToNext()) {
                                                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                                                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                                                    if (phones.isLast()) {
                                                        user_data_ref.child(uid).child("friends").child(nameFormat(name)).setValue(phoneNumberFormat(phoneNumber)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                phones.close();
                                                                String regId = prefs.getString("regId", null);

                                                                user_ref.child(uid).child("fcm_reg_token").setValue(regId);
                                                                user_ref.child(uid).child("name").setValue(user_name_profile.getText().toString());
                                                                user_ref.child(uid).child("phone_number").setValue(currentUser.getPhoneNumber()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        prefs.edit().putBoolean("user_detail_set",true).apply();
                                                                        Intent intent2 = new Intent(SetupProfile.this, MainActivity.class);
                                                                        intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                        intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                        startActivity(intent2);
                                                                        finish();
                                                                    }
                                                                });

                                                            }
                                                        });
                                                    } else {
                                                        user_data_ref.child(uid).child("friends").child(nameFormat(name)).setValue(phoneNumberFormat(phoneNumber));
                                                    }
                                                }
                                            }
                                            else {
                                                prefs.edit().putBoolean("user_detail_set",true).apply();
                                                Intent intent2 = new Intent(SetupProfile.this, MainActivity.class);
                                                intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent2);
                                                finish();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }
        });

        PermissionDialog pd = new PermissionDialog(SetupProfile.this,"main");
        pd.setCancelable(false);
        pd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pd.show();

        pd.setDialogResult(new PermissionDialog.OnMyDialogResult(){
            public void finish(String result){
                if(result.equals("accepted")){
                    ActivityCompat.requestPermissions(SetupProfile.this, new String[]{READ_CONTACTS,READ_EXTERNAL_STORAGE,CAMERA} , MAIN_PERMISSION);
                }
            }
        });
    }

    private void selectImage() {

        final CharSequence[] options = {"Take Photo", "Choose from Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(SetupProfile.this);
        builder.setTitle("Set Profile Picture");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {

                    if (Functions.checkPermission(new String[]{CAMERA}, SetupProfile.this)) {
                       ActivityCompat.requestPermissions(SetupProfile.this, new String[]{CAMERA} , CAMERA_PERMISSION);
                    }
                    else{

                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // Ensure that there's a camera activity to handle the intent
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            // Create the File where the photo should go

                            File newFile = null;
                            try {
                                newFile = createImageFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            fileUri = FileProvider.getUriForFile(SetupProfile.this, "saurav.chandra.baatmessenger.fileprovider", newFile);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                            } else {
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                            }
                            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
                        } else {
                            Toast.makeText(SetupProfile.this, "Cannot connect to camera.", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                else {
                    if (Functions.checkPermission(new String[]{READ_EXTERNAL_STORAGE}, SetupProfile.this)) {
                        ActivityCompat.requestPermissions(SetupProfile.this, new String[]{READ_EXTERNAL_STORAGE} , STORAGE_PERMISSION);
                    }
                    else {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, STORAGE_REQUEST_CODE);
                    }
                }
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Context context = this;
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == CAMERA_REQUEST_CODE) {

                try {
                    Bitmap bitmap_1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                    Bitmap bitmap_1_resized = ThumbnailUtils.extractThumbnail(bitmap_1, 500, 500);
                    BitmapDrawable bdrawable = new BitmapDrawable(SetupProfile.this.getResources(), getclip(bitmap_1_resized));
                    display_pic_select.setText("");
                    display_pic_select.setBackground(bdrawable);
                    photo_data_set = true;
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap_1_resized.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                    byteArray = byteArrayOutputStream.toByteArray();
                    compressedImageFile = new Compressor(SetupProfile.this).compressToFile(new File(fileUri.getPath()));

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {

                try {
                    Uri selectedImage = data.getData();
                    String[] filePath = {MediaStore.Images.Media.DATA};
                    Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                    c.moveToFirst();
                    int columnIndex = c.getColumnIndex(filePath[0]);
                    String picturePath = c.getString(columnIndex);
                    Bitmap bitmap_2 = (BitmapFactory.decodeFile(picturePath));
                    Bitmap bitmap_2_resized = ThumbnailUtils.extractThumbnail(bitmap_2, 500, 500);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap_2_resized.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                    byteArray = byteArrayOutputStream.toByteArray();
                    File f = new File(picturePath);
                    compressedImageFile = new Compressor(SetupProfile.this).compressToFile(f);
                    BitmapDrawable bdrawable = new BitmapDrawable(context.getResources(), getclip(bitmap_2_resized));
                    display_pic_select.setText("");
                    display_pic_select.setBackground(bdrawable);
                    photo_data_set = true;
                    c.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            Log.d("xxx_data","failed");
        }
    }

    public String phoneNumberFormat(String phonenumber) {
        phonenumber = phonenumber.replaceAll("\\s+", "");
        phonenumber = phonenumber.replace("-", "");
        if (phonenumber.substring(0, 1).equals("0")) {
            phonenumber = phonenumber.substring(1);
        }
        if (phonenumber.length() == 10) {
            phonenumber = "+91" + phonenumber;
        }

        return phonenumber;
    }

    private String nameFormat(String namef) {
        namef = namef.replace("#", "");
        namef = namef.replace(".", "");
        namef = namef.replace("$", "");
        namef = namef.replace("[", "");
        namef = namef.replace("]", "");

        return namef;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),"BaatMessenger/Media/Temp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("BaatMessenger", "failed to create directory");
                return null;
            }
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                mediaStorageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {

            case CAMERA_PERMISSION:{

                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Ensure that there's a camera activity to handle the intent
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go

                        File newFile = null;
                        try {
                            newFile = createImageFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        fileUri = FileProvider.getUriForFile(SetupProfile.this, "saurav.chandra.baatmessenger.fileprovider", newFile);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                        } else {
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                        }
                        startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
                    } else {
                        Toast.makeText(SetupProfile.this, "Cannot connect to camera.", Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(SetupProfile.this);

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

            case STORAGE_PERMISSION: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, STORAGE_REQUEST_CODE);
                }
                else{
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(SetupProfile.this);

                        builder.setTitle("Permission Denied");
                        builder.setIcon(R.mipmap.logo);
                        builder.setCancelable(true);
                        builder.setMessage("Storage permission is required to access camera. To continue Allow the permission from settings.");
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
}