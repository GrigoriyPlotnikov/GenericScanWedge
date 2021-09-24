package com.darryncampbell.genericscanwedge.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class App extends Application {
    public static final String ANDROID_CHANNEL_ID = "com.darryncampbell.genericscanwedge.android.Location.Channel";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel serviceChannel = new NotificationChannel(ANDROID_CHANNEL_ID, "Example Service Channel" , NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(serviceChannel);
    }
}
