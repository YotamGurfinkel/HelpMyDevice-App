package com.yotam.customer.helpmydevice;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.regex.Pattern;

import static com.yotam.customer.helpmydevice.Constants.CAMERA_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.CHANNEL_ID;
import static com.yotam.customer.helpmydevice.Constants.EXISTING_POST_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.NEW_POST_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.UPDATE_IMAGE_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.currentPhotoPath;
import static com.yotam.customer.helpmydevice.Constants.currentPostFollowing;
import static com.yotam.customer.helpmydevice.Constants.user;

public class CustomerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, SelectPhotoDialog.OnPhotoSelectedListener, View.OnLongClickListener {
    private DrawerLayout mDrawerLayout;
    private PostsFragment postsFragment;
    private SearchView searchView;
    private ProfileFragment profileFragment;
    private ImageView userImage;
    private static final String TAG = "CustomerActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // hide the keyboard

        if(savedInstanceState != null){
            currentPhotoPath = savedInstanceState.getString("path"); // get the photo path from savedInstanceState
            Log.d(TAG, "onCreate: currentPath " +currentPhotoPath);
            postsFragment = (PostsFragment) getSupportFragmentManager().getFragment(savedInstanceState, "posts"); // get postsFragment from savedInstanceState
        }

        if(!getSharedPreferences("postFollow", Context.MODE_PRIVATE).getAll().isEmpty()){ // check if post followed from sharedpref file
            currentPostFollowing = getSharedPreferences("postFollow", Context.MODE_PRIVATE).getString("following", null);
            startService(new Intent(this, NotificationService.class)); // start following service
        }

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        userImage = navigationView.getHeaderView(0).findViewById(R.id.userImage);
        userImage.setOnClickListener(this);
        userImage.setOnLongClickListener(this);

        mDrawerLayout = findViewById(R.id.customer_drawer);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        createNotificationChannel();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(CustomerActivity.this,mDrawerLayout, toolbar,R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        setDetails(this);

        if(postsFragment == null){
            Log.d(TAG, "onCreate: PostFragment is null");
            postsFragment = new PostsFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, postsFragment).commit();
        navigationView.setCheckedItem(R.id.nav_posts);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("path",currentPhotoPath); // save the photo path to savedInstanceState
        if(postsFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, "posts", postsFragment);
        }


        Log.d(TAG, "onSaveInstanceState: path = " + currentPhotoPath);
    }

    // Function to log out user
    private void logoutUser(){
        final AlertDialog.Builder logoutDialogBuilder = new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setIcon(getResources().getDrawable(R.drawable.ic_menu_logout))
                .setMessage("Are you sure you want to log out?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseAuth.getInstance().signOut();
                        SharedPreferences oldPref = getSharedPreferences("UserDetails", Context.MODE_PRIVATE);
                        oldPref.edit().clear().apply();
                        stopService(new Intent(CustomerActivity.this, NotificationService.class));
                        user = null;
                        startActivity(new Intent(CustomerActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog logoutDialog = logoutDialogBuilder.create();
        logoutDialog.show();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                super.onOptionsItemSelected(item);
                return true;
        }
    }

    // Function to quit the app
    private void quitApp(){
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Quit")
                .setMessage("Are you sure you want to quit the app?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopService(new Intent(CustomerActivity.this, NotificationService.class));
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create().show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        return true;
    }

    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if(!searchView.isIconified()){
            searchView.setIconified(true);
        } else{
            quitApp();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch(menuItem.getItemId()){
            case R.id.nav_profile:
                if(profileFragment==null){
                    profileFragment = new ProfileFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,profileFragment).commit();
                break;
            case R.id.nav_logout:
                logoutUser();
                break;
            case R.id.nav_posts:
                if(postsFragment == null){
                    postsFragment = new PostsFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, postsFragment).commit();
                break;
            case R.id.nav_share:
                share();
                break;
            case R.id.nav_about:
                showAboutDialog();
                break;
        }
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAboutDialog(){
        AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_about);
        aboutBuilder.create().show();
    }

    private void share(){
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = "Look at this awesome app! It's creator is on Instagram!\n" +
                "https://www.instagram.com/yotam_g/";
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "App Recommendation");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share Via"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: Request code is " + requestCode);

        switch (requestCode){
            case NEW_POST_REQUEST_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    createDialog(true, null, getString(R.string.dialog_post));
                }else{
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                    createDialog(false, null, getString(R.string.dialog_post));
                }
                break;
            case EXISTING_POST_REQUEST_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    createDialog(true, PostsFragment.getEditingPost(),getString(R.string.dialog_post));
                }else{
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                    createDialog(false, PostsFragment.getEditingPost(),getString(R.string.dialog_post));
                }
                break;
            case UPDATE_IMAGE_REQUEST_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    new PhotoUploader(false).uploadImage(this);
                }else{
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    // Function to create a notification channel to send notifications about followed post
    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Post Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for the post you followed");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void createDialog(boolean permissionsGranted, Post post, String tag){
        AddPostDialog editPost = new AddPostDialog();
        if(post != null) {
            editPost.setPost(post);
        }
        editPost.setPermissionsGranted(permissionsGranted);
        editPost.show(getSupportFragmentManager(), tag);
    }

    // Static function to set the details for the NavigationDrawer
    public static void setDetails(final Activity activity){
        NavigationView navView = activity.findViewById(R.id.nav_view);
        if(user!=null) {
            Log.d(TAG, "setDetails: user photo url is " + user.getPhotoUrl());
            TextView emailDisplay = navView.getHeaderView(0).findViewById(R.id.emailDisplay);
            emailDisplay.setText(user.getEmail());
            TextView nameDisplay = navView.getHeaderView(0).findViewById(R.id.userFirstLast);
            String[] userDetails = user.getDisplayName().split(Pattern.quote("|"),4);

            nameDisplay.setText(userDetails[0] + " " +userDetails[1]);
            final ImageView userImage = navView.getHeaderView(0).findViewById(R.id.userImage);
            DatabaseReference imgRef = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
            Log.d(TAG, "setDetails: imgRef is " + imgRef);
            imgRef.child("img").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!TextUtils.isEmpty(dataSnapshot.getValue(String.class))) {
                        Glide.with(activity).load(dataSnapshot.getValue(String.class))
                                .apply(RequestOptions.circleCropTransform()).into(userImage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
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

    private void updateUserImage(){
        if(PhotoHelper.verifyPermissions(this, UPDATE_IMAGE_REQUEST_CODE)){
            new PhotoUploader(false).uploadImage(this);
        }
    }

    private void deleteUserImage(){
        if(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
            AlertDialog verifyDelete = new AlertDialog.Builder(this)
                    .setTitle("Delete Image")
                    .setMessage("Are you sure you want to delete your image?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final ProgressDialog deleteProgress = new ProgressDialog(CustomerActivity.this);
                            deleteProgress.setMessage("Deleting Your Image...");
                            deleteProgress.setCancelable(false);
                            deleteProgress.show();

                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder().setPhotoUri(null).build();
                            user.updateProfile(request)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                FirebaseStorage.getInstance().getReference("/user_images/users/" + user.getUid()).delete()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    FirebaseDatabase.getInstance().getReference("users/" + user.getUid()).child("img").removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    deleteProgress.dismiss();
                                                                                    if (task.isSuccessful()) {
                                                                                        Toast.makeText(CustomerActivity.this, "Photo Deleted Successfully", Toast.LENGTH_SHORT).show();
                                                                                        userImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_profile));
                                                                                    } else {
                                                                                        Toast.makeText(CustomerActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                } else {
                                                                    deleteProgress.dismiss();
                                                                    Toast.makeText(CustomerActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            } else {
                                                deleteProgress.dismiss();
                                                Toast.makeText(CustomerActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
            verifyDelete.show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onClick(View v) {
        if(v == userImage){
            updateUserImage();
        }
    }

    @Override
    public void getImagePath(Uri imagePath) {
        Log.d(TAG, "getImagePath: started");
        new PhotoUploader(false).uploadNewPhoto(imagePath, this, "/user_images/users/" + user.getUid());
    }

    @Override
    public void getImageBitmap(Bitmap bitmap) {
        Log.d(TAG, "getImageBitmap: started");
        new PhotoUploader(false).uploadNewPhoto(bitmap, this, "/user_images/users/" + user.getUid());
    }

    @Override
    public boolean onLongClick(View v) {
        if(v == userImage){
            deleteUserImage();
        }

        return true;
    }
}
