package com.yotam.customer.helpmydevice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.yotam.customer.helpmydevice.Constants.CAMERA_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.REQUEST_PERMISSIONS;
import static com.yotam.customer.helpmydevice.Constants.user;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener, SelectPhotoDialog.OnPhotoSelectedListener, TextWatcher {
    private EditText emailRegister,passwordRegister,passwordConfirmation,firstNameInput,lastNameInput,phoneInput;
    private Button registerButton,imageChoose;
    private TextView loginLink;
    private ProgressDialog progressDialog;
    private ImageView mUserImage;
    private Bitmap mSelectedBitmap;
    private Uri mSelectedUri;

    private static final String TAG = "RegisterActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        PhotoHelper.clearStorage(this);

        getSupportActionBar().setTitle("Register");

        emailRegister = findViewById(R.id.emailregisterInput);
        imageChoose = findViewById(R.id.imageChoose);
        progressDialog = new ProgressDialog(this);
        passwordRegister = findViewById(R.id.passwordregisterInput);
        passwordConfirmation = findViewById(R.id.passwordConfirm);
        registerButton = findViewById(R.id.registerButton);
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        loginLink = findViewById(R.id.loginLink);
        imageChoose.setVisibility(View.INVISIBLE);
        mUserImage = findViewById(R.id.chosenView);
        imageChoose.setVisibility(View.GONE);

        if(PhotoHelper.verifyPermissions(this, REQUEST_PERMISSIONS)){
            imageChoose.setVisibility(View.VISIBLE);
        }

        registerButton.setOnClickListener(this);
        loginLink.setOnClickListener(this);
        imageChoose.setOnClickListener(this);
        passwordConfirmation.addTextChangedListener(this);
        passwordRegister.addTextChangedListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            ((DialogFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.dialog_select_photo))).dismiss();
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            getImageBitmap(bitmap);
        }
    }

    private void registerWithDetails(String email, String pass, final String firstName, final String lastName, final String phoneNumber, final String technicianOrUser){

        progressDialog.setMessage("Registering " + technicianOrUser + "...");
        progressDialog.show();
        progressDialog.setCancelable(false);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,pass)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(final AuthResult authResult) {
                        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                .setDisplayName(firstName + "|" + lastName + "|" + phoneNumber + "|" + technicianOrUser).build();
                        authResult.getUser().updateProfile(request)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        user = authResult.getUser();
                                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users/"+ user.getUid());
                                        userRef.setValue(new Customer(firstName, lastName,phoneNumber))
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()){
                                                            Log.d(TAG, "onSuccess: Added To Database");
                                                            PhotoUploader uploader = new PhotoUploader(false);
                                                            if(mSelectedBitmap != null && mSelectedUri == null){
                                                                uploader.uploadNewPhoto(mSelectedBitmap, RegisterActivity.this,"/user_images/users/"+user.getUid());
                                                            }
                                                            else if (mSelectedBitmap == null && mSelectedUri != null){
                                                                uploader.uploadNewPhoto(mSelectedUri, RegisterActivity.this, "/user_images/users/"+user.getUid());
                                                            }

                                                            progressDialog.dismiss();
                                                            progressDialog.setCancelable(true);
                                                            SharedPreferences pref = getSharedPreferences("UserDetails", Context.MODE_PRIVATE);
                                                            SharedPreferences.Editor editor = pref.edit();
                                                            editor.putString("First Name", firstName);
                                                            editor.putString("Last Name", lastName);
                                                            editor.putString("Phone Number", phoneNumber);
                                                            editor.apply();
                                                            startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
                                                            finish();
                                                        }else {
                                                            user.delete()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            Toast.makeText(RegisterActivity.this, "User Registration Failed", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });

                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                progressDialog.setCancelable(true);
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_PERMISSIONS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    imageChoose.setVisibility(View.VISIBLE);
                }else{
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }



    @Override
    public void onClick(View v) {
        if(v == registerButton){
            Log.d(TAG, "onClick: Registering user");
            if(TextUtils.isEmpty(emailRegister.getText().toString().trim())){
                emailRegister.setError("You Must Enter an Email");
            }
            if(TextUtils.isEmpty(passwordRegister.getText().toString())){
                passwordRegister.setError("You Must Enter a Password");
            }
            if(TextUtils.isEmpty(firstNameInput.getText().toString().trim())){
                firstNameInput.setError("You Must Enter Your First Name");
            }

            if(passwordRegister.getText().toString().equals(passwordConfirmation.getText().toString()) &&
                !TextUtils.isEmpty(emailRegister.getText().toString().trim()) && !TextUtils.isEmpty(passwordRegister.getText().toString()) &&
                    !TextUtils.isEmpty(firstNameInput.getText().toString())) {

                AlertDialog askTechnician = new AlertDialog.Builder(this)
                        .setMessage("Are You Registering As a Technician or a User?")
                        .setPositiveButton("User", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                registerWithDetails(emailRegister.getText().toString(), passwordRegister.getText()
                                                .toString(), firstNameInput.getText().toString(),
                                        lastNameInput.getText().toString(), phoneInput.getText().toString(), "User");
                            }
                        }).setNegativeButton("Technician", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                registerWithDetails(emailRegister.getText().toString(), passwordRegister.getText()
                                                .toString(), firstNameInput.getText().toString(),
                                        lastNameInput.getText().toString(), phoneInput.getText().toString(), "Technician");
                            }
                        }).create();
                askTechnician.show();
            }
        }
        else if(v == loginLink){
            startActivity(new Intent(this,LoginActivity.class));
            finish();
        }
        else if (v == imageChoose){
            new PhotoUploader(false).uploadImage(this);
        }
    }

    @Override
    public void getImagePath(Uri imagePath) {
        Log.d(TAG, "getImagePath: setting the image to imageView");
        Glide.with(this).load(imagePath).apply(RequestOptions.circleCropTransform()).into(mUserImage);
        mUserImage.setVisibility(View.VISIBLE);

        mSelectedUri = imagePath;
        mSelectedBitmap = null;
    }

    @Override
    public void getImageBitmap(Bitmap bitmap) {
        Log.d(TAG, "getImageBitmap: setting the image to imageView");
        Glide.with(this).load(bitmap).apply(RequestOptions.circleCropTransform()).into(mUserImage);


        mSelectedUri = null;
        mSelectedBitmap = bitmap;

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if(passwordRegister.getText().toString().length() > 0) {
            if (!passwordConfirmation.getText().toString().equals(passwordRegister.getText().toString())) {
                passwordConfirmation.setError("Password Confirmation is Not The Same");
            } else {
                passwordConfirmation.setError(null);
            }
        }else {
            passwordConfirmation.setError(null);
        }
    }
}
