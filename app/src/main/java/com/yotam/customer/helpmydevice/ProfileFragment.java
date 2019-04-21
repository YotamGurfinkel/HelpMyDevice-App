package com.yotam.customer.helpmydevice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

import static com.yotam.customer.helpmydevice.Constants.user;

public class ProfileFragment extends Fragment implements View.OnClickListener {
    private TextInputLayout firstName,lastName,phoneNumber;
    private Button updateProfile;
    private ProgressDialog progressDialog;
    private static final String TAG = "ProfileFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        firstName = view.findViewById(R.id.firstNameProfile);
        lastName = view.findViewById(R.id.lastNameProfile);
        phoneNumber = view.findViewById(R.id.phoneProfile);
        updateProfile = view.findViewById(R.id.updateButton);
        progressDialog = new ProgressDialog(getActivity());

        updateProfile.setOnClickListener(this);

        initialize();

        return view;
    }

    private void initialize(){
        if(user != null){
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserDetails" , Context.MODE_PRIVATE);

            firstName.getEditText().setText(sharedPreferences.getString("First Name", ""));
            lastName.getEditText().setText(sharedPreferences.getString("Last Name", ""));
            phoneNumber.getEditText().setText(sharedPreferences.getString("Phone Number", ""));
        }
    }

    private void changeProfileDetails(final String first, final String last, final String phone){
        progressDialog.setMessage("Updating Profile...");
        progressDialog.setCancelable(false);
        if(TextUtils.isEmpty(first)) {
            firstName.setError("First Name Can't Be Empty");
        }else {
            if(getActivity().getCurrentFocus() != null) {
                ((InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                getActivity().getCurrentFocus().clearFocus();
            }
            progressDialog.show();
            String[] currentUserDetails = user.getDisplayName().split(Pattern.quote("|"),4);
            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                    .setDisplayName(first + "|" + last + "|" + phone + "|" + currentUserDetails[3]).build();
            user.updateProfile(request)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
                            Customer customer;
                            if(user.getPhotoUrl() != null){
                                customer = new Customer(first, last, user.getPhotoUrl().toString(), phone);
                            }else{
                                customer = new Customer(first, last, phone);
                            }
                            userRef.setValue(customer)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            progressDialog.dismiss();
                                            if(task.isSuccessful()){
                                                Toast.makeText(getActivity(), "Profile Details Changed Successfully", Toast.LENGTH_SHORT).show();
                                                CustomerActivity.setDetails(getActivity());
                                            }
                                            else{
                                                Toast.makeText(getActivity(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initialize();
        firstName.setErrorEnabled(false);
    }

    @Override
    public void onClick(View v) {
        if(v == updateProfile){
            SharedPreferences pref = getActivity().getSharedPreferences("UserDetails", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            firstName.setErrorEnabled(false);
            
            if(firstName.getEditText().getText().toString().equals(pref.getString("First Name", "def")) &&
                lastName.getEditText().getText().toString().equals(pref.getString("Last Name", "def")) &&
                phoneNumber.getEditText().getText().toString().equals(pref.getString("Phone Number", "def"))){
                Toast.makeText(getContext(), "Details Are The Same", Toast.LENGTH_SHORT).show();
            }
            else if(!TextUtils.isEmpty(firstName.getEditText().getText().toString()) && !TextUtils.isEmpty(lastName.getEditText().getText().toString())) {
                String first = firstName.getEditText().getText().toString();
                String last = lastName.getEditText().getText().toString();
                editor.putString("First Name", first);
                editor.putString("Last Name", last);
                editor.putString("Phone Number", phoneNumber.getEditText().getText().toString());
                editor.apply();
                changeProfileDetails(first,last,phoneNumber.getEditText().getText().toString());
            }
            else if(TextUtils.isEmpty(firstName.getEditText().getText().toString())){
                firstName.setError("You must enter your first name");
            }
            else if(TextUtils.isEmpty(lastName.getEditText().getText().toString())){
                String first = firstName.getEditText().getText().toString();
                editor.putString("First Name", first);
                editor.putString("Last Name", "");
                editor.putString("Phone Number", phoneNumber.getEditText().getText().toString());
                editor.apply();
                changeProfileDetails(first, "", phoneNumber.getEditText().getText().toString());
            }


        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Profile");
    }
}
