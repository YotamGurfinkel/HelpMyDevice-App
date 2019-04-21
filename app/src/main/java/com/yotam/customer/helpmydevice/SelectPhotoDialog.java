package com.yotam.customer.helpmydevice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import static com.yotam.customer.helpmydevice.Constants.CAMERA_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.PICKFILE_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.currentPhotoPath;

public class SelectPhotoDialog extends DialogFragment {

    private static final String TAG = "SelectPhotoDialog";
    private boolean isGoodQuality = true;

    public interface OnPhotoSelectedListener{
        void getImagePath(Uri imagePath);
        void getImageBitmap(Bitmap bitmap);
    }

    OnPhotoSelectedListener mOnPhotoSelectedListener;

    public void setGoodQuality(boolean isGoodQuality){
        this.isGoodQuality = isGoodQuality;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_selectphoto, container, false);

        TextView selectPhoto = view.findViewById(R.id.dialogChoosePhoto);
        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Accessing phone's memory....");
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICKFILE_REQUEST_CODE);
            }
        });

        TextView takePhoto = view.findViewById(R.id.dialogOpenCamera);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Starting Camera....");
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(isGoodQuality) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Error Taking Image", Toast.LENGTH_SHORT).show();
                    }
                    if (photoFile != null) {
                         Uri photoURI = FileProvider.getUriForFile(getContext(),
                                "com.yotam.customer.helpmydevice.fileprovider",
                                photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        getActivity().getSupportFragmentManager().findFragmentByTag(getString(R.string.dialog_post)).startActivityForResult(intent , CAMERA_REQUEST_CODE);
                    }
                }else{
                    getActivity().startActivityForResult(intent, CAMERA_REQUEST_CODE);
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri selectImageUri = data.getData();
            Log.d(TAG, "onActivityResult: image uri: " + selectImageUri);
            mOnPhotoSelectedListener.getImagePath(selectImageUri);
            getDialog().dismiss();
        }
        else if(requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            getDialog().dismiss();
        }
    }

    private File createImageFile() throws IOException{
        PhotoHelper.clearAll(getContext());
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Calendar.getInstance().getTimeInMillis());
        String imageFileName = DateFormat.format("yyyyMMdd_HHmmss",cal).toString();
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "createImageFile: image path is " + image.getAbsolutePath());
        Log.d(TAG, "createImageFile: file is in " + currentPhotoPath);
        return image;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach: activated");
        if(getTargetFragment()==null){
            Log.d(TAG, "onAttach: getTargetFragment is null");
            try{
                mOnPhotoSelectedListener = (OnPhotoSelectedListener)context;
                Log.d(TAG, "onAttach: try: tried");
            }catch (ClassCastException e){
                Log.e(TAG, "onAttach: ClassCastException: " +e.getMessage());
            }
        }
        else {
            Log.d(TAG, "onAttach: getTargetFragment is not null");
            Log.d(TAG, "onAttach: fragment is " + getTargetFragment());
            try{
                mOnPhotoSelectedListener = (OnPhotoSelectedListener)getTargetFragment();
            }catch (ClassCastException e){
                Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage());
            }
        }

    }


}
