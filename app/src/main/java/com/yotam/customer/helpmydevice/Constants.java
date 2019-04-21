package com.yotam.customer.helpmydevice;

import com.google.firebase.auth.FirebaseUser;

// Class with a few Constants and static variables
public class Constants {
    public static FirebaseUser user;
    static String currentPhotoPath;
    static String currentPostFollowing;
    static final String CHANNEL_ID = "following";
    static final int CAMERA_REQUEST_CODE = 4321;
    static final int PICKFILE_REQUEST_CODE = 1234;
    static final int REQUEST_PERMISSIONS = 666;
    static final int NEW_POST_REQUEST_CODE = 667;
    static final int EXISTING_POST_REQUEST_CODE = 668;
    static final int UPDATE_IMAGE_REQUEST_CODE = 669;
}
