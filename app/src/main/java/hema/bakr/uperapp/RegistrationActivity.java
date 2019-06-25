package hema.bakr.uperapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {
    private EditText mEmail, mPassword , mPasswordConfirm;
    private RadioButton mCustomer , mDriver;
    private FirebaseAuth mAuth;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mProgressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mPasswordConfirm = findViewById(R.id.password_confirm);
        mCustomer = findViewById(R.id.rbCustomer);
        mDriver = findViewById(R.id.rbDriver);


        Button mRegistration = findViewById(R.id.registration);

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String email = mEmail.getText().toString();
                final String pass = mPassword.getText().toString();
                final String passConfirm = mPasswordConfirm.getText().toString();

                if (validateInput(email, pass , passConfirm)) {
                    //Show progress dialog to prevent user from interacting with the screen
                    mProgressDialog.setMessage("Signing up");
                    mProgressDialog.show();
                    mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(RegistrationActivity.this, "Sign Up Error..", Toast.LENGTH_SHORT).show();
                                mProgressDialog.dismiss();
                            } else {
                                String user_id = mAuth.getCurrentUser().getUid();

                                String userType = "";
                                if (mDriver.isChecked()){
                                    userType = "Riders";
                                }
                                else userType = "Customers";

                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child(userType).child(user_id);
                                current_user_db.setValue(true);
                                mProgressDialog.dismiss();
                                gotoMap(userType);
                            }
                        }
                    });
                }
            }
        });

    }

    private void gotoMap(String userType) {
        Intent intent;
        if (userType.equals("Riders")){
            intent = new Intent(RegistrationActivity.this , DriverSettingsActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            intent = new Intent(RegistrationActivity.this , CustomerSettingsActivity.class);
            startActivity(intent);
            finish();
        }
    }

    //This function validates the user input
    private boolean validateInput(String mail, String password , String passwordConfirm) {

        //Validating the email input
        if (!TextUtils.isEmpty(mail)) {
            if (!isValidEmailId(mail.trim())) {
                mEmail.setError("Please enter a valid Email address");
                return false;
            }
        } else {
            mEmail.setError("Field can't be empty");
            return false;
        }

        if (!TextUtils.isEmpty(password)) {
            if (password.length() < 6) {
                mPassword.setError("Password must be more than 5 characters");
                return false;
            }
        } else {
            mPassword.setError("Field can't be empty");
            return false;
        }

        if(!password.equals(passwordConfirm)){
            mPasswordConfirm.setError("Password confirmation doesn't match");
            return false;
        }

        if ( !mCustomer.isChecked() && !mDriver.isChecked()){
            mCustomer.setError("You must choose your type");
            return false;
        }
        else return true;

    }

    //Regular expression for checking mail
    private boolean isValidEmailId(String email) {

        return Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$").matcher(email).matches();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}
