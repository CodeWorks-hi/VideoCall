package com.silmedy.videocall;

import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG        = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "call_channel";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "새 FCM 토큰: " + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "🔥 FCM received: " + remoteMessage.getData());
        Log.d(TAG, "🔥 FCM notification: " + remoteMessage.getNotification());

        String roomId = remoteMessage.getData().get("roomId");
        Intent intent = new Intent("com.silmedy.videocall.ACTION_INCOMING_CALL");
        intent.setPackage(getPackageName());
        intent.putExtra("roomId", roomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("영상 통화 요청")
                .setContentText("수락하려면 탭하세요.")
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pi);

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), nb.build());
    }
}