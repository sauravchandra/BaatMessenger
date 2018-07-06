package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraActivity extends Activity{

    private String uid, chat_with_uid, chat_with_name, chat_with_fcm_id, chat_with_phone_number;

    private Context myContext;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;

    private Button flashButton;
    private Button captureButton;
    private Button switchCameraButton;
    private static Camera mCamera;
    private PictureCallback mPicture;
    private CameraPreview mPreview;
    private static boolean cameraFront = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");
        chat_with_uid = intent.getStringExtra("chat_with_uid");
        chat_with_name = intent.getStringExtra("chat_with_name");
        chat_with_phone_number = intent.getStringExtra("chat_with_phone_number");
        chat_with_fcm_id = intent.getStringExtra("chat_with_fcm_id");

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);
        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.hide();
        }

        FirebaseDatabase database = ((BaatMessenger) this.getApplication()).getDatabase();
        myConnectionsRef = database.getReference("presenceStatus/" + uid + "/connected");
        lastOnlineRef = database.getReference("presenceStatus/" + uid + "/lastOnline");

        myContext = this;
        initialize();
    }

    private void initialize() {

        mCamera=Camera.open(0);
        cameraFront=false;

        FrameLayout cameraPreview = (FrameLayout) findViewById(R.id.camera_view);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview, 0);

        final Camera.Parameters params = mCamera.getParameters();

        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size size = determineBestSize(sizes);

        params.setPreviewSize(size.width, size.height);

        List<String> focusModes = params.getSupportedFocusModes();

        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        mCamera.enableShutterSound(true);
        mCamera.setParameters(params);

        captureButton = (Button) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(captureListener);

        flashButton = (Button) findViewById(R.id.flash_button);
        flashButton.setVisibility(View.GONE);
        //flashButton.setOnClickListener(flashListener);

        switchCameraButton = (Button) findViewById(R.id.camera_change_button);
        switchCameraButton.setOnClickListener(cameraSwitchListener);
    }

    private View.OnClickListener cameraSwitchListener = new View.OnClickListener(){
        @Override
        public void onClick (View v){
            switchCamera();
        }
    };

    private View.OnClickListener flashListener = new View.OnClickListener(){

        @Override
        public void onClick (View v){

            Camera.Parameters params = mCamera.getParameters();

            if(params.getFlashMode()!=null){
            if (params.getFlashMode().equals("off"))
            {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                mCamera.setParameters(params);
                flashButton.setBackgroundResource(R.drawable.flash_on);
            }
            else if(params.getFlashMode().equals("on")){
                params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                mCamera.setParameters(params);
                flashButton.setBackgroundResource(R.drawable.flash_auto);
            }
            else if(params.getFlashMode().equals("auto")){
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(params);
                flashButton.setBackgroundResource(R.drawable.flash_off);
            }
        }
        else
        {
            Toast toast = Toast.makeText(myContext, "Flash mode change failed", Toast.LENGTH_LONG);
            toast.show();}
        }
    };

    private View.OnClickListener captureListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            buttonEffect(captureButton);
            mCamera.takePicture(shutterCallback, null, mPicture);
        }
    };

    private void resumeCamera() {
        if (cameraFront) {
            mCamera = Camera.open(0);
            cameraFront=true;
            final Camera.Parameters params = mCamera.getParameters();

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size size = determineBestSize(sizes);

            params.setPreviewSize(size.width, size.height);
            params.setPictureSize(size.width, size.height);

            List<String> focusModes = params.getSupportedFocusModes();

            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            mCamera.enableShutterSound(true);
            mCamera.setParameters(params);

            captureButton = (Button) findViewById(R.id.capture_button);
            captureButton.setOnClickListener(captureListener);

            flashButton = (Button) findViewById(R.id.flash_button);
            flashButton.setOnClickListener(flashListener);

            switchCameraButton = (Button) findViewById(R.id.camera_change_button);
            switchCameraButton.setOnClickListener(cameraSwitchListener);

            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
        else {
            mCamera = Camera.open(1);
            cameraFront=false;
            final Camera.Parameters params = mCamera.getParameters();

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size size = determineBestSize(sizes);

            params.setPreviewSize(size.width, size.height);
            params.setPictureSize(size.width, size.height);

            List<String> focusModes = params.getSupportedFocusModes();

            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            mCamera.enableShutterSound(true);
            mCamera.setParameters(params);

            captureButton = (Button) findViewById(R.id.capture_button);
            captureButton.setOnClickListener(captureListener);

            flashButton = (Button) findViewById(R.id.flash_button);
            flashButton.setOnClickListener(flashListener);

            switchCameraButton = (Button) findViewById(R.id.camera_change_button);
            switchCameraButton.setOnClickListener(cameraSwitchListener);

            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
    }

    private void chooseCamera() {
        if (cameraFront) {
            mCamera = Camera.open(1);
            cameraFront=false;
            final Camera.Parameters params = mCamera.getParameters();

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size size = determineBestSize(sizes);

            params.setPreviewSize(size.width, size.height);
            params.setPictureSize(size.width, size.height);

            List<String> focusModes = params.getSupportedFocusModes();

            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            mCamera.enableShutterSound(true);
            mCamera.setParameters(params);

            captureButton = (Button) findViewById(R.id.capture_button);
            captureButton.setOnClickListener(captureListener);

            flashButton = (Button) findViewById(R.id.flash_button);
            flashButton.setVisibility(View.GONE);
            //flashButton.setOnClickListener(flashListener);

            switchCameraButton = (Button) findViewById(R.id.camera_change_button);
            switchCameraButton.setOnClickListener(cameraSwitchListener);

            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
        else {
            mCamera = Camera.open(0);
            cameraFront=true;
            final Camera.Parameters params = mCamera.getParameters();

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size size = determineBestSize(sizes);

            params.setPreviewSize(size.width, size.height);
            params.setPictureSize(size.width, size.height);

            List<String> focusModes = params.getSupportedFocusModes();

            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            mCamera.enableShutterSound(true);
            mCamera.setParameters(params);

            captureButton = (Button) findViewById(R.id.capture_button);
            captureButton.setOnClickListener(captureListener);

            flashButton = (Button) findViewById(R.id.flash_button);
            flashButton.setVisibility(View.VISIBLE);
            flashButton.setOnClickListener(flashListener);

            switchCameraButton = (Button) findViewById(R.id.camera_change_button);
            switchCameraButton.setOnClickListener(cameraSwitchListener);

            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
    }

    private void switchCamera(){
            releaseCamera();
            chooseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myConnectionsRef.setValue(Boolean.TRUE);
        releaseCamera();
        resumeCamera();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            myConnectionsRef.setValue(Boolean.FALSE);
            lastOnlineRef.setValue(ServerValue.TIMESTAMP);
        }
    }


    private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {

        }
    };

    private PictureCallback getPictureCallback() {

        return new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File pictureFile = getOutputMediaFile();
                if (pictureFile == null) {
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);

                    Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                    if(cameraFront){
                        int side=1;
                        realImage=rotate(realImage,90, side);
                    }
                    else
                    {
                        int side=0;
                        realImage=rotate(realImage,270,side);
                    }

                    realImage.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                    fos.close();

                    Intent intent3 = new Intent(CameraActivity.this, PhotoSend.class);
                    intent3.putExtra("uid", uid);
                    intent3.putExtra("chat_with_uid", chat_with_uid);
                    intent3.putExtra("photo_uri", Uri.fromFile(pictureFile).getPath());
                    intent3.putExtra("chat_with_name", chat_with_name);
                    intent3.putExtra("chat_with_phone_number",chat_with_phone_number);
                    intent3.putExtra("chat_with_fcm_id", chat_with_fcm_id);
                    startActivity(intent3);
                    finish();

                } catch (IOException e) {
                }

            }

            Bitmap rotate(Bitmap src, float degree, int cside)
            {
                // create new matrix
                Matrix matrix = new Matrix();
                // setup rotation degree
                matrix.setRotate(degree);
                if(cside==0){matrix.postScale(-1, 1, src.getWidth()/2f, src.getHeight()/2f);}
                Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
                return bmp;
            }
        };
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),"BaatMessenger/Media/Sent");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("BaatMessenger", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath()  + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }


    private static Camera.Size determineBestSize(List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMemory = Runtime.getRuntime().maxMemory() - used;
        for (Camera.Size currentSize : sizes) {
            int newArea = currentSize.width * currentSize.height;
            long neededMemory = newArea * 4 * 4; // newArea * 4 Bytes/pixel * 4 needed copies of the bitmap (for safety :) )
            boolean isDesiredRatio = (currentSize.width / 16) == (currentSize.height / 9);
            boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
            boolean isSafe = neededMemory < availableMemory;
            if (isDesiredRatio && isBetterSize && isSafe) {
                bestSize = currentSize;
            }
        }
        if (bestSize == null) {
            return sizes.get(0);
        }
        return bestSize;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent4= new Intent(getApplicationContext(),Chat.class);
        intent4.putExtra("chat_with_uid",chat_with_uid);
        intent4.putExtra("chat_with_name",chat_with_name);
        intent4.putExtra("chat_with_fcm_id",chat_with_fcm_id);
        intent4.putExtra("chat_with_phone_number",chat_with_phone_number);
        startActivity(intent4);
        finish();
    }

    private void releaseCamera() {
       if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private static void buttonEffect(View button){
        button.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        v.getBackground().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }
}

