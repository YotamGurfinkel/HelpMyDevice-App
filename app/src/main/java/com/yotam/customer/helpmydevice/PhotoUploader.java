package com.yotam.customer.helpmydevice;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class PhotoUploader {

    private static final String TAG = "PhotoUploader";
    private boolean goodQuality;

    public PhotoUploader(boolean goodQuality){
        this.goodQuality = goodQuality;
    }

    public void uploadNewPhoto(Bitmap bitmap, Context context, String path){
        Log.d(TAG, "uploadNewPhoto: uploading a new image bitmap to storage");
        BackgroundImageResize resize = new BackgroundImageResize(bitmap, context, path);
        Uri uri = null;
        resize.execute(uri);
    }

    public void uploadNewPhoto(Uri imagePath, Context context,String path){
        Log.d(TAG, "uploadNewPhoto: uploading a new image uri to storage");
        BackgroundImageResize resize = new BackgroundImageResize(null, context, path);
        resize.execute(imagePath);
    }

    public void updatePostPhoto(Uri imagePath, Context context, String path, Dialog dialog){
        BackgroundImageResize resize = new BackgroundImageResize(null, context, path);
        resize.setUpdating(true);
        resize.setDialog(dialog);
        resize.execute(imagePath);
    }

    public void uploadPost(Uri imagePath, Context context,String path,Post post, Dialog dialog){
        Log.d(TAG, "uploadNewPhoto: uploading a new image uri to storage");
        BackgroundImageResize resize = new BackgroundImageResize(null, context, path, post, dialog);
        resize.execute(imagePath);
    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    public void uploadImage(Context context){
        Log.d(TAG, "uploadImage: opening Dialog to choose new photo, got context");
        SelectPhotoDialog dialog = new SelectPhotoDialog();
        dialog.setGoodQuality(goodQuality);
        FragmentActivity fragmentActivity = (FragmentActivity) context;
        dialog.show(fragmentActivity.getSupportFragmentManager(),context.getString(R.string.dialog_select_photo));
    }

    public void uploadImage(Fragment fragment){
        Log.d(TAG, "uploadImage: opening Dialog to choose new photo, got fragment");
        SelectPhotoDialog dialog = new SelectPhotoDialog();
        FragmentActivity fragmentActivity = fragment.getActivity();
        if(fragmentActivity == null || fragment.getContext() == null){
            Log.e(TAG, "uploadImage: fragmentActivity = null and getContext() = null");
        }else {
            dialog.setTargetFragment(fragment, 1);
            dialog.show(fragment.getFragmentManager(), fragment.getContext().getString(R.string.dialog_select_photo));
        }


    }
}
