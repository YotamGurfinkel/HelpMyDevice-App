package com.yotam.customer.helpmydevice;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.yotam.customer.helpmydevice.Constants.CHANNEL_ID;
import static com.yotam.customer.helpmydevice.Constants.currentPostFollowing;
import static com.yotam.customer.helpmydevice.Constants.user;

public class NotificationService extends Service {
    private static final String TAG = "NotificationService";
    private DatabaseReference postRef;
    private ChildEventListener postStateListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: postId: " + currentPostFollowing);
        getSharedPreferences("postFollow", Context.MODE_PRIVATE).edit().putString("following", currentPostFollowing).apply();
        postRef = FirebaseDatabase.getInstance().getReference("posts/"+ currentPostFollowing);
        postStateListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d(TAG, "onChildChanged: " + dataSnapshot.getKey());
                if(dataSnapshot.getKey().equals("solved")){
                    if(dataSnapshot.getValue(Boolean.class).equals(true)){
                        Log.d(TAG, "onChildChanged: post marked solved");
                        notifyPost("Post Solved", "The Post You Followed Was Solved!");
                    }else {
                        Log.d(TAG, "onChildChanged: post edited");
                        notifyPost("Post Edited", "The Post You Followed Was Edited!");
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getKey().equals("title")){
                    notifyPost("Post Deleted", "The Post You Followed Was Deleted");
                    stopSelf();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        postRef.addChildEventListener(postStateListener);
        return START_STICKY;
    }

    private void notifyPost(String title, String description){

        Intent notifyIntent = new Intent(this, CustomerActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(description)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.technician_icon)
                .setContentIntent(notifyPendingIntent)
                .build();

        notificationManager.notify(1, notification);
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "onDestroy: SERVICE DESTROYED");
        super.onDestroy();
        postRef.removeEventListener(postStateListener);
        getSharedPreferences("postFollow", Context.MODE_PRIVATE).edit().clear().apply();
        currentPostFollowing = null;
    }
}
