// ReceiveService.java
package com.silmedy.videocall;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.*;

/**
 * 포그라운드 서비스로서 Firebase RTDB 감시
 * offer 감지 시 알림 생성
 */
public class ReceiveService extends Service {
    private String userId;
    private DatabaseReference callRef;
    private ValueEventListener offerListener;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getStringExtra("userId");
        Log.d("ReceiveService", "🚀 서비스 시작됨, userId=" + userId);
        callRef = FirebaseDatabase.getInstance()
                .getReference("calls")
                .child(userId);
        listenForCalls();
        // 서비스 지속용 낮은 우선순위 알림
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("Silmedy 대기 중")
                .setContentText("영상진료 알림 대기")
                .setPriority(NotificationCompat.PRIORITY_LOW);
        startForeground(NotificationHelper.NOTIFY_ID, notif.build());
        return START_STICKY;
    }

    private void listenForCalls() {
        offerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sdp = snapshot.getValue(String.class);
                    showCallNotification(sdp);
                    callRef.child("offer").removeEventListener(this);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        };
        callRef.child("offer").addValueEventListener(offerListener);
    }

    private void showCallNotification(String offerSdp) {
        Log.d("ReceiveService", "📢 showCallNotification() 호출됨, offerSdp 존재 여부: " + (offerSdp != null));
        Intent intent = new Intent(this, ReceiveActivity.class);
        intent.putExtra("roomId", userId);
        intent.putExtra("offerSdp", offerSdp);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("영상진료 호출")
                .setContentText("의사님의 호출을 수락해주세요.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(NotificationHelper.NOTIFY_ID + 1, builder.build());
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        super.onDestroy();
        if (callRef != null && offerListener != null) {
            callRef.child("offer").removeEventListener(offerListener);
        }
    }
}