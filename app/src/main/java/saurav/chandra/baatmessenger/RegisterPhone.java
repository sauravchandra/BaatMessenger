package saurav.chandra.baatmessenger;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import mbanje.kurt.fabbutton.FabButton;

public class RegisterPhone extends Activity {

    private static final String TAG = "PhoneAuthActivity";

    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";

    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private TextView VerifyText;

    private EditText mCountryCodeField,mPhoneNumberField;
    private EditText mVerificationField;
    private TextView instantVerification;
    private TextView waitingForOTP;

    private FabButton mStartButton;
    private ImageView mVerifyButton;
    private Button mResendButton;

    private ViewGroup mCodeSend;
    private ViewGroup mCodeVerify;

    private int seconds = 59;
    private int minutes = 0;

    ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
        setContentView(R.layout.activity_phone_register);

        ActionBar actionbar= getActionBar();
        actionbar.hide();

        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // Assign views
        mCodeSend = (ViewGroup) findViewById(R.id.code_send);
        mCodeVerify = (ViewGroup) findViewById(R.id.code_verify);

        mCountryCodeField = (EditText) findViewById(R.id.country_code);
        mPhoneNumberField = (EditText) findViewById(R.id.phone_number);
        mVerificationField =(EditText) findViewById(R.id.verify_code);
        instantVerification = (TextView) findViewById(R.id.instant_validation);
        waitingForOTP = (TextView) findViewById(R.id.waiting_for_otp);

        mStartButton = (FabButton) findViewById(R.id.send_code_button);
        mVerifyButton = (ImageView) findViewById(R.id.verify_button);
        mResendButton = (Button) findViewById(R.id.resend_button);

        VerifyText = (TextView) findViewById(R.id.verify_text);

        final ProgressHelper helper = new ProgressHelper(mStartButton,this);

        // Assign click listeners
        mStartButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!validatePhoneNumber()) {
                    return;
                }
                helper.startIndeterminate();
                startPhoneNumberVerification(mCountryCodeField.getText().toString()+mPhoneNumberField.getText().toString());
            }
        });

        mVerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = mVerificationField.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    mVerificationField.setError("Cannot be empty.");
                    return;
                }
                verifyPhoneNumberWithCode(mVerificationId, code);
            }
        });

        mResendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seconds=59;
                minutes=0;
                resendVerificationCode(mCountryCodeField.getText().toString()+mPhoneNumberField.getText().toString(), mResendToken);
            }
        });

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // Initialize phone auth callbacks
        // [START phone_auth_callbacks]
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verificaiton without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                // [END_EXCLUDE]

                // [START_EXCLUDE silent]
                // Update the UI and attempt sign in with the phone credential
                updateUI(STATE_VERIFY_SUCCESS, credential);
                // [END_EXCLUDE]
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                // [END_EXCLUDE]

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // [START_EXCLUDE]
                    mPhoneNumberField.setError("Invalid phone number.");
                    // [END_EXCLUDE]
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // [START_EXCLUDE]
                    Log.e("Error","SMS Quota exceeded");
                    // [END_EXCLUDE]
                }

                // Show a message and update the UI
                // [START_EXCLUDE]
                updateUI(STATE_VERIFY_FAILED);
                // [END_EXCLUDE]
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                // [START_EXCLUDE]
                // Update UI
                updateUI(STATE_CODE_SENT);
                // [END_EXCLUDE]
            }
        };
        // [END phone_auth_callbacks]
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);


        // [START_EXCLUDE]
        if (mVerificationInProgress && validatePhoneNumber()) {
            startPhoneNumberVerification(mCountryCodeField.getText().toString()+mPhoneNumberField.getText().toString());
        }
        // [END_EXCLUDE]
    }
    // [END on_start_check_user]

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

        mVerificationInProgress = true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        // [START verify_with_code]
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential);
    }

    // [START resend_verification]
    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }
    // [END resend_verification]

    // [START sign_in_with_phone]
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // [START_EXCLUDE]
                            updateUI(STATE_SIGNIN_SUCCESS, user);
                            // [END_EXCLUDE]
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                // [START_EXCLUDE silent]
                                mVerificationField.setError("Invalid code.");
                                // [END_EXCLUDE]
                            }
                            // [START_EXCLUDE silent]
                            // Update UI
                            updateUI(STATE_SIGNIN_FAILED);
                            // [END_EXCLUDE]
                        }
                    }
                });
    }
    // [END sign_in_with_phone]

    private void updateUI(int uiState) {
        updateUI(uiState, mAuth.getCurrentUser(), null);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            updateUI(STATE_SIGNIN_SUCCESS, user);
        } else {
            updateUI(STATE_INITIALIZED);
        }
    }

    private void updateUI(int uiState, FirebaseUser user) {
        updateUI(uiState, user, null);
    }

    private void updateUI(int uiState, PhoneAuthCredential cred) {
        updateUI(uiState, null, cred);
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {

        switch (uiState) {
            case STATE_INITIALIZED:
                // Initialized state, show only the phone number field and start button
                mCodeSend.setVisibility(View.VISIBLE);
                mCodeVerify.setVisibility(View.GONE);
                break;

            case STATE_CODE_SENT:
                View view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                mCodeSend.setVisibility(View.GONE);
                mCodeVerify.setVisibility(View.VISIBLE);
                mResendButton.setVisibility(View.GONE);
                instantVerification.setVisibility(View.GONE);

                waitingForOTP.setVisibility(View.VISIBLE);

                VerifyText.setText("Verify  "+mCountryCodeField.getText().toString()+mPhoneNumberField.getText().toString());

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        waitingForOTP.setText("Waiting for OTP  "+String.valueOf(1)+":"+String.valueOf(00));
                    }
                }, 1000);

                //Declare the timer
                final Timer t = new Timer();
                //Set the schedule function and rate
                t.scheduleAtFixedRate(new TimerTask() {

                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                TextView tv = (TextView) findViewById(R.id.waiting_for_otp);

                                if(seconds==0)
                                {
                                    mResendButton.setVisibility(View.VISIBLE);
                                    waitingForOTP.setVisibility(View.GONE);
                                    t.cancel();
                                    t.purge();
                                }

                                if(String.valueOf(seconds).length()==1 && seconds!=0){
                                    tv.setText("Waiting for OTP  "+String.valueOf(minutes)+":0"+String.valueOf(seconds));
                                    seconds -= 1;
                                }
                                else {
                                    tv.setText("Waiting for OTP  "+String.valueOf(minutes)+":"+String.valueOf(seconds));
                                    seconds -= 1;
                                }
                            }

                        });
                    }

                }, 0, 1000);

                break;

            case STATE_VERIFY_FAILED:
                // Verification has failed, show all options
                mCodeSend.setVisibility(View.VISIBLE);
                mCodeVerify.setVisibility(View.GONE);
                break;

            case STATE_VERIFY_SUCCESS:
                view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                // Verification has succeeded, proceed to firebase sign in
                mCodeSend.setVisibility(View.GONE);
                mCodeVerify.setVisibility(View.VISIBLE);
                mVerifyButton.setVisibility(View.VISIBLE);
                mResendButton.setVisibility(View.GONE);

                // Set the verification text based on the credential
                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        waitingForOTP.setVisibility(View.GONE);
                        mVerificationField.setText(cred.getSmsCode());
                    } else {
                        mVerificationField.setText("Instan");
                        mVerificationField.setVisibility(View.GONE);
                        instantVerification.setVisibility(View.VISIBLE);
                        instantVerification.setText("Instant Validation");
                    }
                }

                break;
            case STATE_SIGNIN_FAILED:
                mCodeSend.setVisibility(View.VISIBLE);
                mCodeVerify.setVisibility(View.GONE);

                // No-op, handled by sign-in check
                break;
            case STATE_SIGNIN_SUCCESS:
                Intent intent = new Intent(this, SetupProfile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                // Np-op, handled by sign-in check
                break;
        }

    }

    private boolean validatePhoneNumber() {
        String phoneNumber = mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            mPhoneNumberField.setError("Empty phone number.");
            return false;
        }

        return true;
    }

    private void enableViews(View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    private void disableViews(View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }
}