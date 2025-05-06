package com.silmedy.videocall;

import static org.webrtc.VideoFrameDrawer.TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;

/**
 * ▶ FCM 알림 클릭 또는 Home→진입 시 호출
 * ▶ 로그인 체크, RTDB에서 offer SDP 읽고 WebRTC 방 참여
 */
public class ReceiveActivity extends AppCompatActivity {
    private static final String PREFS     = "SilmedyPrefs";
    private static final String KEY_TOKEN = "access_token";

    private SurfaceViewRenderer remoteView, localView;
    private WebRTCManager webRTC;
    private EglBase eglBase;

    // ◀ 여기부터: Firebase 종료 감지용 필드
    private DatabaseReference callRef;
    private ValueEventListener callListener;
    // ▶ 여기까지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚪 ReceiveActivity created");
        // 1) 최초 인텐트 처리
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent intent extras=" + intent.getExtras());
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        String roomId   = intent.getStringExtra("roomId");
        boolean isCaller = intent.getBooleanExtra("isCaller", false);
        Log.d(TAG, "handleIntent() roomId=" + roomId + ", isCaller=" + isCaller);

        if (token == null || roomId == null) {
            Log.e(TAG, "❌ roomId가 null입니다. 통화 초기화 불가!");
            startActivity(new Intent(this, LoginActivity.class)
                    .putExtra("after_login_roomId", roomId));
            finish();
            return;
        }

        // 레이아웃 + 오디오 세팅
        setContentView(R.layout.activity_receive);
        remoteView = findViewById(R.id.remoteView);
        localView  = findViewById(R.id.localView);
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(true);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        // WebRTC 렌더러 초기화
        eglBase = EglBase.create();
        remoteView.init(eglBase.getEglBaseContext(), null);
        remoteView.setMirror(false);
        localView.init(eglBase.getEglBaseContext(), null);
        localView.setMirror(true);

        // FCM 풀스크린 알림 취소
        NotificationManagerCompat.from(this)
                .cancel(NotificationHelper.NOTIFY_ID + 1);

        // WebRTC 연결 시작
        webRTC = new WebRTCManager(this, eglBase, remoteView, localView);
        webRTC.setRoomId(roomId);

        // ◀ 여기부터: Firebase 경로 삭제 시 액티비티 종료 감지 설정
        callRef = FirebaseDatabase
                .getInstance()
                .getReference("calls")
                .child(roomId);
        callListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "📴 방 데이터가 삭제되었습니다. ReceiveActivity 종료.");
                    // WebRTC 리소스 해제
                    webRTC.dispose();
                    // 액티비티 종료
                    finish();
                }
            }
            @Override public void onCancelled(DatabaseError error) { }
        };
        callRef.addValueEventListener(callListener);
        // ▶ 여기까지

        if (isCaller) {
            Log.d(TAG, "📞 Caller 역할 - Offer 생성 시작");
            webRTC.createOfferAndSend(roomId);
        } else {
            Log.d(TAG, "📥 Callee 역할 - Offer 수신 대기");
            webRTC.createAnswerAndSend(roomId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webRTC != null) webRTC.dispose();
        if (remoteView != null) remoteView.release();
        if (localView != null) localView.release();
        if (eglBase != null) eglBase.release();

        // ◀ 여기부터: 리스너 해제
        if (callRef != null && callListener != null) {
            callRef.removeEventListener(callListener);
        }
        // ▶ 여기까지
    }
}