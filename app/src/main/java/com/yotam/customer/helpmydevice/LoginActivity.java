package com.yotam.customer.helpmydevice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

import static com.yotam.customer.helpmydevice.Constants.user;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private Button loginButton;
    private TextInputLayout emailInput,passwordInput;
    private TextView registerLink, forgotPassLink;
    private ProgressDialog progressDialog;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        PhotoHelper.clearStorage(this);

        getSupportActionBar().setTitle("Login");

        if(FirebaseAuth.getInstance().getCurrentUser()!=null){
            user = FirebaseAuth.getInstance().getCurrentUser();
            Log.d(TAG, "onCreate: user is " +user.getEmail());
            SharedPreferences preferences = getSharedPreferences("UserDetails", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            String[] userDetails = user.getDisplayName().split(Pattern.quote("|"),4);
            Log.d(TAG, "onCreate: user details " + userDetails);
            editor.putString("First Name", userDetails[0]);
            editor.putString("Last Name", userDetails[1]);
            editor.putString("Phone Number", userDetails[2]);
            editor.apply();
            startActivity(new Intent(this, CustomerActivity.class));
            finish();
        }

        loginButton = findViewById(R.id.loginButton);
        emailInput = findViewById(R.id.emailInput);
        forgotPassLink = findViewById(R.id.forgotPassLink);
        passwordInput = findViewById(R.id.passwordInput);
        registerLink = findViewById(R.id.registerLink);
        progressDialog = new ProgressDialog(this);

        registerLink.setOnClickListener(this);
        forgotPassLink.setOnClickListener(this);
        loginButton.setOnClickListener(this);
    }

    private void loginWithDetails(String email, String pass){

        emailInput.setErrorEnabled(false);
        passwordInput.setErrorEnabled(false);
        if (getCurrentFocus() != null) {
            ((InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            getCurrentFocus().clearFocus();
        }

        progressDialog.setMessage("Logging in...");
        progressDialog.show();
        progressDialog.setCancelable(false);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                user = FirebaseAuth.getInstance().getCurrentUser();
                                SharedPreferences preferences = getSharedPreferences("UserDetails", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                String[] userDetails = user.getDisplayName().split(Pattern.quote("|"), 4);
                                editor.putString("First Name", userDetails[0]);
                                editor.putString("Last Name", userDetails[1]);
                                editor.putString("Phone Number", userDetails[2]);
                                editor.commit();
                                progressDialog.dismiss();
                                user = FirebaseAuth.getInstance().getCurrentUser();
                                startActivity(new Intent(LoginActivity.this, CustomerActivity.class));
                                finish();

                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }

                        }
                });
        }

    private void forgotPassword(String email){
        final ProgressDialog forgotPassProgress = new ProgressDialog(this);
        forgotPassProgress.setCancelable(false);
        forgotPassProgress.setMessage("Sending Password Reset Email...");
        forgotPassProgress.show();
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(LoginActivity.this, "Password Reset Email Has Been Sent", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        forgotPassProgress.dismiss();
                    }
                });
    }

    @Override
    public void onClick(View v) {
        if(v == loginButton){
            if(TextUtils.isEmpty(emailInput.getEditText().getText().toString().trim())){
                emailInput.setError("Please Enter Your Email");
            }else{
                emailInput.setErrorEnabled(false);
            }

            if(TextUtils.isEmpty(passwordInput.getEditText().getText().toString())){
                passwordInput.setError("Please Enter Your Password");
            }else{
                passwordInput.setErrorEnabled(false);
            }

            if(!TextUtils.isEmpty(emailInput.getEditText().getText().toString()) && !TextUtils.isEmpty(passwordInput.getEditText().getText().toString())) {
                loginWithDetails(emailInput.getEditText().getText().toString(), passwordInput.getEditText().getText().toString());
            }

        }
        else if(v == registerLink){
            startActivity(new Intent(this,RegisterActivity.class));
            finish();
        }
        else if(v == forgotPassLink){
            if(passwordInput.isErrorEnabled()){
                passwordInput.setErrorEnabled(false);
            }
            if(TextUtils.isEmpty(emailInput.getEditText().getText().toString())){
                emailInput.setError("Please Enter Your Email");
            }else {
                emailInput.setErrorEnabled(false);
                forgotPassword(emailInput.getEditText().getText().toString());
            }
        }
    }
}
