package hema.bakr.uperapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CustomerSettingsActivity extends AppCompatActivity {


    private EditText mNameField;
    private EditText mPhoneFiled;
    private DatabaseReference mCustomerDatabase;
    private String mName;
    private String mPhone;
    private String userID;
    private ImageView mProfileImage;
    private Uri profileImageUri;
    private String mProfileImageUrl;
    ProgressDialog mProgressDialog;

    public static final String CUSTOMER_PREFES = "CusPrefes";
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        mProgressDialog = new ProgressDialog(this);

        mNameField = findViewById(R.id.name);
        mPhoneFiled = findViewById(R.id.phone);

        Button mConfirm = findViewById(R.id.confirm);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();


        mProfileImage = findViewById(R.id.profile_image);
        //This listener gets the user profile image from the gallery
        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                //This request code "1" is an identifier for handling multiple intents with results
                startActivityForResult(intent, 1);
            }
        });


        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userID);

        getUserInfo();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                saveUserInformation();
            }
        });


    }

    private void getUserInfo() {

        mProgressDialog.setMessage("Please wait ...");
        mProgressDialog.show();

        mCustomerDatabase.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map != null) {
                        if (map.get("name") != null) {
                            mName = Objects.requireNonNull(map.get("name")).toString();
                            mNameField.setText(mName);
                        }
                        if (map.get("phone") != null) {
                            mPhone = Objects.requireNonNull(map.get("phone")).toString();
                            mPhoneFiled.setText(mPhone);
                        }
                        if (map.get("profileImageUrl") != null) {
                            mProfileImageUrl = Objects.requireNonNull(map.get("profileImageUrl")).toString();
                            Glide.with(getApplication()).load(mProfileImageUrl).into(mProfileImage);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        mProgressDialog.dismiss();
    }


    // function to update the data entered
    private void saveUserInformation() {

        mProgressDialog.setMessage("Saving your information");
        mProgressDialog.show();

        mName = mNameField.getText().toString();
        mPhone = mPhoneFiled.getText().toString();

        if (TextUtils.isEmpty(mName)){
            mNameField.setError("You must enter your name");
            mProgressDialog.dismiss();
            return;
        }

        if ( TextUtils.isEmpty(mPhone) ){
            mNameField.setError("You must enter your mobile number");
            mProgressDialog.dismiss();
            return;
        }

        Map<String, Object> userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);

        mCustomerDatabase.updateChildren(userInfo);
        if (profileImageUri != null) {
            //This creates a child for each user to save its image but on firebase storage not database
            final StorageReference imagePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userID);

            //Here we convert the Uri we have to a meaningful Bitmap in order to save it to the firebase storage
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), profileImageUri);
            } catch (IOException e) {
                Log.e("Error", "Can't transform Uri to image");
            }

            //Here we compress the image size and convert it to JPEG format then store it in an array of bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);

                byte[] data = baos.toByteArray();
                //Now we upload the image to the firebase storage reference that we chose before
                UploadTask uploadTask = imagePath.putBytes(data);
                //This listener checks when the upload is completed successfully then add the image download URL to the
                //firebase database as child of the customer
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imagePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Map newImage = new HashMap();
                                newImage.put("profileImageUrl", uri.toString());
                                mCustomerDatabase.updateChildren(newImage);
                                mProgressDialog.dismiss();

                                Intent intent = new Intent(CustomerSettingsActivity.this , CustomerMapActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                });
                //This listener is called when upload fails
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Error", "Error uploading the image");
                        mProgressDialog.dismiss();
                        Intent intent = new Intent(CustomerSettingsActivity.this , CustomerMapActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        } else {
            mProgressDialog.dismiss();
            Intent intent = new Intent(CustomerSettingsActivity.this , CustomerMapActivity.class);
            startActivity(intent);
            finish();
        }

        editor = getSharedPreferences(CUSTOMER_PREFES, MODE_PRIVATE).edit();
        editor.putString("name", mName);
        editor.putString("phone", mPhone);
        editor.apply();
    }

    //This function is called when the user choose a profile image from gallery
    //This function set the image view to the new pic and get the Uri of the image on the device
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                profileImageUri = data.getData();
                mProfileImage.setImageURI(profileImageUri);

                editor = getSharedPreferences(CUSTOMER_PREFES, MODE_PRIVATE).edit();
                editor.putString("image",profileImageUri.toString());
                editor.apply();
            }
            else{
                Toast.makeText(CustomerSettingsActivity.this , "You must choose an image " , Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {

        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}
