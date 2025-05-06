package com.silmedy.videocall;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.IceCandidate;

/**
 * RTDB 기반 Signaling 클라이언트
 * - offer/answer/ICE 후보의 송수신을 담당
 * - WebRTCManager 콜백에 매핑
 */
public class FirebaseSignalingClient {
    public interface Callback {
        void onOfferReceived(String sdp);
        void onAnswerReceived(String sdp);
        void onIceCandidateReceived(IceCandidate candidate);
    }

    private static final String TAG = "FirebaseSignalingClient";
    private final DatabaseReference rootRef;
    private final Callback callback;
    private ValueEventListener offerListener, answerListener;
    private ChildEventListener callerCandidatesListener, calleeCandidatesListener;

    /** 생성자: roomId 경로 구독 시작 */
    public FirebaseSignalingClient(String roomId, Callback callback) {
        Log.d(TAG, "📥 생성자 호출됨. roomId = " + roomId);
        if (roomId == null || roomId.isEmpty()) {
            Log.e(TAG, "❌ roomId가 null 또는 빈 문자열입니다! child() 호출 시 NPE 발생!");
        }
        this.callback = callback;
        this.rootRef = FirebaseDatabase.getInstance()
                .getReference("calls")
                .child(roomId);
        listen();
    }

    /** 모든 리스너 등록 */
    private void listen() {
        // ① Offer 수신
        offerListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                if (!snap.exists()) return;
                Object raw = snap.getValue();
                String sdp = raw instanceof String
                        ? (String) raw
                        : ((java.util.Map<?,?>) raw).get("sdp").toString();
                Log.d(TAG, "📥 Offer 수신: " + sdp);
                callback.onOfferReceived(sdp);
            }
            @Override public void onCancelled(DatabaseError e) {
                Log.e(TAG, "Offer listener cancelled", e.toException());
            }
        };
        rootRef.child("offer").addValueEventListener(offerListener);

        // ② Answer 수신
        answerListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                if (!snap.exists()) return;
                Object raw = snap.getValue();
                String sdp = raw instanceof String
                        ? (String) raw
                        : ((java.util.Map<?,?>) raw).get("sdp").toString();
                Log.d(TAG, "📥 Answer 수신: " + sdp);
                callback.onAnswerReceived(sdp);
            }
            @Override public void onCancelled(DatabaseError e) {
                Log.e(TAG, "Answer listener cancelled", e.toException());
            }
        };
        rootRef.child("answer").addValueEventListener(answerListener);

        // ③ Caller ICE 후보 수신
        callerCandidatesListener = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot snap, String prev) {
                String type = snap.child("type").getValue(String.class);
                if ("ice".equals(type)) {
                    String mid = snap.child("sdpMid").getValue(String.class);
                    Integer idx = snap.child("sdpMLineIndex").getValue(Integer.class);
                    String cand = snap.child("sdp").getValue(String.class);
                    Log.d(TAG, "📥 Caller ICE candidate: " + cand);
                    if (mid != null && idx != null && cand != null) {
                        IceCandidate candidate = new IceCandidate(mid, idx, cand);
                        callback.onIceCandidateReceived(candidate);
                    }
                }
            }
            @Override public void onChildChanged(DataSnapshot snap, String prev) {}
            @Override public void onChildRemoved(DataSnapshot snap) {}
            @Override public void onChildMoved(DataSnapshot snap, String prev) {}
            @Override public void onCancelled(DatabaseError e) {
                Log.e(TAG, "CallerCandidates listener cancelled", e.toException());
            }
        };
        rootRef.child("callerCandidates").addChildEventListener(callerCandidatesListener);

        // ④ Callee ICE 후보 수신
        calleeCandidatesListener = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot snap, String prev) {
                SignalingData data = snap.getValue(SignalingData.class);
                if (data != null && data.hasCandidate()) {
                    IceCandidate candidate = data.toIceCandidate();
                    Log.d(TAG, "📥 Callee ICE candidate: " + candidate.sdp);
                    callback.onIceCandidateReceived(candidate);
                }
            }
            @Override public void onChildChanged(DataSnapshot snap, String prev) {}
            @Override public void onChildRemoved(DataSnapshot snap) {}
            @Override public void onChildMoved(DataSnapshot snap, String prev) {}
            @Override public void onCancelled(DatabaseError e) {
                Log.e(TAG, "CalleeCandidates listener cancelled", e.toException());
            }
        };
        rootRef.child("calleeCandidates").addChildEventListener(calleeCandidatesListener);
    }

    /** Offer 전송 (의사) */
    public void sendOffer(String sdp) {
        Log.d(TAG, "📤 sendOffer: " + sdp);
        rootRef.child("offer").setValue(sdp);
    }

    /** Answer 전송 (환자) */
    public void sendAnswer(String sdp) {
        Log.d(TAG, "📤 sendAnswer: " + sdp);
        rootRef.child("answer").setValue(sdp);
    }

    /** ICE 후보 전송 */
    public void sendIceCandidate(SignalingData data) {
        String node = data.isOffer() ? "callerCandidates" : "calleeCandidates";
        Log.d(TAG, "📤 sendIceCandidate to " + node + ": " + data.toString());
        rootRef.child(node).push().setValue(data);
    }

    /** 모든 리스너 해제 */
    public void stop() {
        rootRef.child("offer").removeEventListener(offerListener);
        rootRef.child("answer").removeEventListener(answerListener);
        rootRef.child("callerCandidates").removeEventListener(callerCandidatesListener);
        rootRef.child("calleeCandidates").removeEventListener(calleeCandidatesListener);
        Log.d(TAG, "🛑 All listeners removed");
    }

    /**
     * Doctor가 통화 거절 버튼을 눌렀을 때 호출
     * calls/{roomId}/rejected 에 true를 설정합니다.
     */
    public static void rejectCall(String roomId) {
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("calls")
                .child(roomId)
                .child("rejected");
        ref.setValue(true);
        Log.d(TAG, "❌ Call rejected for roomId=" + roomId);
    }
}