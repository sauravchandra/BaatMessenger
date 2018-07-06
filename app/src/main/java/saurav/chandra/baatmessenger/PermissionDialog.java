package saurav.chandra.baatmessenger;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static android.support.v4.content.ContextCompat.getObbDirs;
import static saurav.chandra.baatmessenger.Functions.askForPermission;

public class PermissionDialog extends Dialog implements
        android.view.View.OnClickListener {

    private Activity c;
    private Dialog d;
    private Button yes, no;
    private TextView permission_textview;
    private ImageView permission_imageview;
    private String permission;
    private OnMyDialogResult mDialogResult;

    protected PermissionDialog(Activity a, String permission) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        this.permission = permission;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.permission_dialog);

        permission_textview = (TextView) findViewById(R.id.permission_text);
        permission_imageview = (ImageView) findViewById(R.id.permission_image);

        switch (permission) {
            case "main":
                permission_textview.setText(R.string.permission_main);
                break;
            case "camera":
                break;
            case "files":
                break;
            case "contacts":
                break;
        }

        yes = (Button) findViewById(R.id.permission_accept);
        no = (Button) findViewById(R.id.permission_delay);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.permission_accept:
                mDialogResult.finish("accepted");
                dismiss();
                break;
            case R.id.permission_delay:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }

    void setDialogResult(OnMyDialogResult dialogResult){
        mDialogResult = dialogResult;
    }

    public interface OnMyDialogResult{
        void finish(String result);
    }
}