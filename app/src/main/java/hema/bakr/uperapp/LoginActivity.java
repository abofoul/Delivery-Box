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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {
    private EditText mEmail, mPassword;
    private RadioButton mCustomer , mDriver;

    private FirebaseAuth mAuth;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mProgressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();


        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mCustomer = findViewById(R.id.rbCustomer);
        mDriver = findViewById(R.id.rbDriver);

        TextView mNewAccount = findViewById(R.id.new_account);
        mNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this , RegistrationActivity.class);
                startActivity(intent);
            }
        });


        Button mLogin = findViewById(R.id.login);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String email = mEmail.getText().toString();
                final String pass = mPassword.getText().toString();

                if (validateInput(email, pass)) {
                    mProgressDialog.setMessage("Logging in");
                    mProgressDialog.show();

                    mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                mProgressDialog.dismiss();
                                Toast.makeText(LoginActivity.this, "Sign In Error..", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                mProgressDialog.dismiss();
                                String userType = "";
                                if (mDriver.isChecked()){
                                    userType = "Riders";
                                }
                                else userType = "Customers";
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
            intent = new Intent(LoginActivity.this , DriverMapActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            intent = new Intent(LoginActivity.this , CustomerMapActivity.class);
            startActivity(intent);
            finish();
        }
    }


    //This function validates the user input
    private boolean validateInput(String mail, String password) {

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
