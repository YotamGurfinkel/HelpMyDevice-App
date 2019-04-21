package com.yotam.customer.helpmydevice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.Calendar;

import static com.yotam.customer.helpmydevice.Constants.CAMERA_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.currentPhotoPath;
import static com.yotam.customer.helpmydevice.Constants.user;

public class AddPostDialog extends DialogFragment implements View.OnClickListener, DialogInterface, SelectPhotoDialog.OnPhotoSelectedListener {
    private static final String TAG = "AddPostDialog";
    private EditText titleInput,descriptionInput;
    private Button imgSelect, deletePhoto;
    private PhotoUploader uploader;
    private boolean isGranted = false;
    private Uri mSelectedUri;
    private Post editingPost = null;
    private ProgressDialog postProgress;
    private ImageView postImg;

    public View onCreateDialogView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_post, container); // inflate here
    }

    @Override
    public void onClick(View v) {
        if(v == imgSelect){
            uploader.uploadImage(this); //initiate uploadImage function with fragment in PhotoUploader class
        }
        else if(v == deletePhoto){
            showDeleteImageDialog();
        }
    }

    @Override
    public void getImagePath(Uri imagePath) {
        Log.d(TAG, "getImagePath: setting the image to imageView");
        Glide.with(this).load(imagePath).into(postImg);

        mSelectedUri = imagePath; // set the image path from the listener in SelectPhotoDialog
    }

    @Override
    public void getImageBitmap(Bitmap bitmap) {
        // empty
    }


    public void setPost(Post post){
        editingPost = post;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //Get the dialog view
        Log.d(TAG, "onCreateDialog: dialog created");
        final View view = onCreateDialogView(getActivity().getLayoutInflater(), null, null);
        titleInput = view.findViewById(R.id.postTitleInput);
        descriptionInput = view.findViewById(R.id.postDescriptionInput);
        deletePhoto = view.findViewById(R.id.deletePhoto);
        imgSelect = view.findViewById(R.id.addPostImage);
        postImg = view.findViewById(R.id.postImage);
        uploader = new PhotoUploader(true);
        imgSelect.setVisibility(View.INVISIBLE); //set Select Image button visibility to INVISIBLE until permissions are granted

        if(PhotoHelper.checkPermissions(getContext()))
            imgSelect.setVisibility(View.VISIBLE); //set Select Image button visibility to VISIBLE since permissions granted

        imgSelect.setOnClickListener(this);
        deletePhoto.setOnClickListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) // build the dialog
                .setPositiveButton("OK", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick: Pressed yes");
                    }
                })
                .setNegativeButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel(); // dismiss the dialog since user cancelled
                    }
                });

        // Set a progress dialog to track the progress of post upload
        postProgress = new ProgressDialog(getContext());
        postProgress.setMessage("Uploading Post...");
        postProgress.setCancelable(false);


        onViewCreated(view, null);
        builder.setView(view); // set the dialog view to the view inflated in OnCreateDialogView

        if(savedInstanceState != null && savedInstanceState.getString("id") != null) { // check if savedInstanceState has an edited post saved in it
            editingPost = new Post(savedInstanceState.getString("title"), savedInstanceState.getString("description"),
                    savedInstanceState.getString("userId"), savedInstanceState.getString("id"), savedInstanceState.getString("userEmail"));
            if (savedInstanceState.getString("editingImg") != null) { // check if savedInstanceState has an edited post's image in it
                editingPost.setImg(savedInstanceState.getString("editingImg"));
            }
        }

        if(editingPost != null){
            ((EditText)view.findViewById(R.id.postTitleInput)).setText(editingPost.getTitle());
            ((EditText)view.findViewById(R.id.postDescriptionInput)).setText(editingPost.getDescription());
            builder.setTitle("Edit Post");
        }else{
            builder.setTitle("Add Post");
        }
        return builder.create(); // return the Dialog


    }

    public void setPermissionsGranted(boolean isGranted){
        this.isGranted = isGranted;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && resultCode != Activity.RESULT_OK){ // the user didn't take a picture or taking a picture didn't succeed
            currentPhotoPath = null;
        }
        else {
            ((DialogFragment) getFragmentManager().findFragmentByTag(getString(R.string.dialog_select_photo))).dismiss(); // dismiss the SelectPhotoDialog
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog d = (AlertDialog)getDialog(); // gets the dialog
        if(d != null /*checks if the dialog is null */){
            d.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.dialogBG)));
            Log.d(TAG, "onResume: current path is " + currentPhotoPath);
            Log.d(TAG, "onResume: isGranted? " + isGranted);
            if(currentPhotoPath != null){ // checks if a photo has been taken
                Glide.with(this).load(currentPhotoPath).into(postImg);
                mSelectedUri = Uri.fromFile(new File(currentPhotoPath));
            }
            if(editingPost != null && editingPost.getImg() != null && mSelectedUri == null) { // checks if the current editing post has an image
                deletePhoto.setVisibility(View.VISIBLE);
                Glide.with(this).load(editingPost.getImg()).into(postImg);
            }else {
                deletePhoto.setVisibility(View.INVISIBLE);
            }
            final Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE); // gets the positive button from the dialog
            // bypass the onClickListener of the dialog positive button
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(TextUtils.isEmpty(titleInput.getText().toString().trim())){
                        titleInput.setError("Title can't be empty");
                    }
                    if(TextUtils.isEmpty(descriptionInput.getText().toString().trim())){
                        descriptionInput.setError("Description can't be empty");
                    }
                    if(!TextUtils.isEmpty(titleInput.getText().toString().trim())&&!TextUtils.isEmpty(descriptionInput.getText().toString().trim())){
                        if(editingPost != null && titleInput.getText().toString().equals(editingPost.getTitle()) && descriptionInput.getText().toString().equals(editingPost.getDescription())
                            && mSelectedUri == null){
                            Toast.makeText(getContext(), "No Details Changed", Toast.LENGTH_SHORT).show();
                        } else {
                            final String key;
                            if (editingPost != null) {
                                key = editingPost.getPostId();
                            } else {
                                key = FirebaseDatabase.getInstance().getReference().push().getKey(); // get a unique key from Firebase database
                            }

                            String path = "/posts/" + key; // set the path

                            if (mSelectedUri != null /* checks if Uri was selected (New photo selected) */) {
                                // Create the post with selected user attributes, and upload it to the database with the Uri
                                if(editingPost != null){
                                    uploader.updatePostPhoto(mSelectedUri, getContext(), path, getDialog());
                                    updateEditingPost(titleInput.getText().toString(), descriptionInput.getText().toString(), path);
                                }else {
                                    Post post = new Post(titleInput.getText().toString(), descriptionInput.getText().toString(), user.getUid(), key, user.getEmail());
                                    uploader.uploadPost(mSelectedUri, getContext(), path, post, d);
                                }

                            } else /* check if no new image was selected */ {
                                postProgress.show(); // show ProgressDialog
                                Log.d(TAG, "onClick: uid is " + user.getUid());
                                Post post;
                                if (editingPost != null) {
                                    updateEditingPost(titleInput.getText().toString(), descriptionInput.getText().toString(), path);
                                } else {
                                    // Create a post with selected user attributes, and upload it to the database without an image

                                    post = new Post(titleInput.getText().toString(), descriptionInput.getText().toString(), user.getUid(), key, user.getEmail());
                                    FirebaseDatabase.getInstance().getReference("posts").child(key).setValue(post)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful() /* checks if upload task was successful */) {
                                                        getDialog().dismiss(); // dismiss the dialog
                                                        Toast.makeText(getActivity(), "Post Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                                    } else /* checks if upload task failed */ {
                                                        Toast.makeText(getActivity(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                    postProgress.dismiss(); // dismiss the ProgressDialog
                                                }
                                            });
                                }

                            }
                        }
                    }
                }
            });
        }
    }

    // Function that updates the editing post's title,description and image
    private void updateEditingPost(final String title, final String description, String path){
        final DatabaseReference postPath = FirebaseDatabase.getInstance().getReference(path);
        postPath.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Post post = dataSnapshot.getValue(Post.class);
                post.setTitle(title);
                post.setDescription(description);
                post.setEdited(true);
                post.setSolved(false);
                post.setTimestamp(Calendar.getInstance().getTimeInMillis());
                postPath.setValue(post).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        postProgress.dismiss();
                        if(task.isSuccessful()){
                            Toast.makeText(getContext(), "Post Updated Successfully", Toast.LENGTH_SHORT).show();
                            if(mSelectedUri == null){
                                getDialog().dismiss();
                            }
                        }else{
                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Function that deletes the current editing post's image
    private void showDeleteImageDialog(){
        final DatabaseReference currentRef = FirebaseDatabase.getInstance().getReference("/posts/"+editingPost.getPostId()); // reference to the current edited post path
        AlertDialog verifyDelete = new AlertDialog.Builder(getContext())
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this post's image?")
                .setPositiveButton("Yes", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog deleteProgress = new ProgressDialog(getContext());
                        deleteProgress.setMessage("Deleting Post Image...");
                        deleteProgress.setCancelable(false);
                        deleteProgress.show();
                        FirebaseStorage.getInstance().getReference("/posts/"+editingPost.getPostId()).delete() // delete the post image from storage
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            currentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    Post post = dataSnapshot.getValue(Post.class);
                                                    post.setImg(null);
                                                    post.setEdited(true);
                                                    post.setTimestamp(Calendar.getInstance().getTimeInMillis());
                                                    currentRef.setValue(post).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            deleteProgress.dismiss();
                                                            if(task.isSuccessful()){
                                                                Toast.makeText(getContext(), "Post Image Deleted Successfully", Toast.LENGTH_SHORT).show();
                                                            }else{
                                                                Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });

                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    deleteProgress.dismiss();
                                                }
                                            });
                                        }else{
                                            deleteProgress.dismiss();
                                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton("No", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        verifyDelete.show();
    }


    @Override
    public void cancel() {
        // empty function. only required by implementing DialogInterface
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        PhotoHelper.clearAll(getContext()); // clear storage from images
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        PhotoHelper.clearAll(getContext()); // clear storage from images
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: started");

        // Check if currently editing post and save its details in savedInstanceState
        if(editingPost != null){
            outState.putString("editingImg", editingPost.getImg());
            outState.putString("title", editingPost.getTitle());
            outState.putString("description", editingPost.getDescription());
            outState.putString("id", editingPost.getPostId());
            outState.putString("userId", editingPost.getUserId());
            outState.putString("userEmail", editingPost.getEmail());
        }

    }
}
