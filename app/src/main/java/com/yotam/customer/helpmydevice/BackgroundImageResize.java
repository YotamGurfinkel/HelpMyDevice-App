package com.yotam.customer.helpmydevice;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import static com.yotam.customer.helpmydevice.Constants.user;
import static com.yotam.customer.helpmydevice.PhotoUploader.getBytesFromBitmap;

public class BackgroundImageResize extends AsyncTask<Uri,Integer,byte[]> {
    private Bitmap bitmap;
    private static final String TAG = "BackgroundImageResize";
    private WeakReference<Context> context;
    private double progress;
    private byte[] uploadBytes;
    private String path;
    private ProgressDialog progressDialog;
    private Post post;
    private Dialog dialog;
    private boolean updating = false;


    // Constructor for BackgroundImageResize without a post
    public BackgroundImageResize(Bitmap bitmap, Context context, String path){
        if(bitmap != null /* checks if bitmap passed isn't null */){
            this.bitmap = bitmap;
        }
        this.context = new WeakReference<>(context); // create a WeakReference for not leaking the context
        this.path = path; // sets the path to the path passed
    }

    // Constructor for BackgroundImageResize with a post
    public BackgroundImageResize(Bitmap bitmap, Context context, String path, Post post, Dialog dialog){
        if(bitmap != null /* checks if bitmap passed isn't null */){
            this.bitmap = bitmap;
        }
        this.context = new WeakReference<>(context); // create a WeakReference for not leaking the context
        this.path = path;
        this.post = post;
        this.dialog = dialog;
    }

    public void setUpdating(boolean isUpdating){
        updating = isUpdating;
    }

    public void setDialog(Dialog dialog){
        this.dialog = dialog;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.progressDialog = new ProgressDialog(context.get()); // create a new ProgressDialog before executing the AsyncTask
    }


    @Override
    protected byte[] doInBackground(Uri... params) {
        Log.d(TAG, "doInBackground: started.");

        if(bitmap == null /* checks if bitmap is null */){
            // Bitmap is null. Uri was passed
            try{
                bitmap = MediaStore.Images.Media.getBitmap(context.get().getContentResolver(),params[0]);// get the bitmap from selected Uri
                File file = new File(params[0].getPath());
                file.delete();
            }catch (IOException e /* catch the exception of getting the bitmap */){
                Log.e(TAG, "doInBackground: IOException " + e.getMessage());
            }
        }
        byte[] bytes = null; // set a byte array to null
        Log.d(TAG, "doInBackground: MB before compression: " +bitmap.getByteCount()/1000000);
        bytes = getBytesFromBitmap(bitmap, 70); // get byte array from bitmap

        Log.d(TAG, "doInBackground: MB after compression: " +bytes.length/1000000);

        return bytes;

    }

    @Override
    protected void onPostExecute(byte[] bytes) {
        super.onPostExecute(bytes);

        if(post == null /* checks if no post was passed */)
            executeUploadTask(bytes); // execute the upload task with the byte array only
        else // post was passed
            executeUploadTask(bytes, post); // execute the upload task with the byte array and post passed

    }

    private void executeUploadTask(byte[] bytes){
        // Executing the upload task with byte array only
        Toast.makeText(this.context.get(), "Uploading Image...", Toast.LENGTH_SHORT).show();

        // Create a storage reference using the path passed
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference(path);

        UploadTask uploadTask = storageReference.putBytes(bytes); // upload the byte array to the storage reference created

        progressDialog.setCancelable(false);
        progressDialog.show();

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //insert the download url into the database
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(final Uri uri) {

                        if(!updating) {
                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri).build(); // create a request to change the user details with the download url got
                            FirebaseAuth.getInstance().getCurrentUser().updateProfile(request) // update user's profile
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "onSuccess: firebase download url: " + uri.toString());
                                            // Set the image path to the database for future use
                                            FirebaseDatabase.getInstance().getReference("users/" + user.getUid()).child("img").setValue(uri.toString())
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            progressDialog.dismiss();
                                                            if (task.isSuccessful() /* checks if database was updated successfully */) {
                                                                Toast.makeText(context.get(), "Photo Uploaded Successfully", Toast.LENGTH_SHORT).show();

                                                            } else /* checks if database update failed */ {
                                                                Toast.makeText(context.get(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        }
                                    });
                        } else{
                            FirebaseDatabase.getInstance().getReference(path).child("img").setValue(uri.toString())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            progressDialog.dismiss();
                                            if(task.isSuccessful()){
                                                Toast.makeText(context.get(), "Post Image Updated Successfully", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            }else{
                                                Toast.makeText(context.get(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }

                    }
                });




            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)  /* checks if upload task failed */{
                progressDialog.dismiss(); // dismiss the ProgressDialog
                Toast.makeText(context.get(), "Upload Failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the current upload progress and set it to the ProgressDialog every 15%
                double currentProgress = (100* taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressDialog.setMessage("0 % done");
                progressDialog.show();
                if( currentProgress > progress + 15){
                    progress = (100* taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "onProgress: upload is " + progress + " & done");
                    progressDialog.setMessage(currentProgress +" % " + "done");

                }
            }
        });
    }

    private void executeUploadTask(byte[] bytes , final Post post){
        // Execute the upload task with byte array and post passed
        progressDialog = new ProgressDialog(context.get());
        progressDialog.setMessage("Uploading Image...");
        progressDialog.setCancelable(false);
        progressDialog.show();


        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(path); // create a storage reference using the path passed

        UploadTask uploadTask = storageReference.putBytes(bytes); // create an upload task to upload the post image

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot /* image of the post uploaded successfully */) {
                Toast.makeText(context.get(), "Photo Uploaded Successfully", Toast.LENGTH_SHORT).show();
                progressDialog.setMessage("Uploading Post...");

                //insert the download url into the database
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Uri downloadUrl = uri; // get the download url from the task
                        Log.d(TAG, "onSuccess: firebase download url: "+downloadUrl.toString());
                        post.setImg(uri.toString()); // set the image to the post object

                        // Insert the post object into the database
                        FirebaseDatabase.getInstance().getReference().child(path).setValue(post)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful() /* checks if update database task was successful */){
                                            Toast.makeText(context.get(), "Post Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss(); // dismiss the dialog passed
                                        }else /* update database task failed */{
                                            Toast.makeText(context.get(), "Post Upload Failed", Toast.LENGTH_SHORT).show();
                                        }
                                        progressDialog.dismiss(); // dismiss the ProgressDialog
                                    }
                                });
                    }
                });



            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) { // checks if the upload task failed
                progressDialog.dismiss(); // dismiss the progress dialog
                Toast.makeText(context.get(), "Upload Failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the current upload progress and set it to the ProgressDialog every 15%
                double currentProgress = (100* taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                if( currentProgress > progress + 15){
                    progress = (100* taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "onProgress: upload is " + progress + " & done");
                    progressDialog.setMessage(currentProgress +" % " + "done");
                }
            }
        });
    }
}
