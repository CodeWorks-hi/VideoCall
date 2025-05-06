package com.silmedy.videocall;

import org.webrtc.IceCandidate;
import com.google.firebase.database.IgnoreExtraProperties;

/**
 * ▶ Firebase signaling message 모델 클래스
 */
@IgnoreExtraProperties
public class SignalingData {
    public String type;          // "offer", "answer", "ice"
    public String sdp;           // offer/answer SDP or ICE sdp
    public String sdpMid;        // ICE candidate only
    public int sdpMLineIndex;    // ICE candidate only

    // 🔹 Firebase 직렬화를 위한 빈 생성자
    public SignalingData() {}

    // 🔹 ICE 후보 전송용 생성자
    public SignalingData(IceCandidate candidate) {
        this.type = "ice";
        this.sdp = candidate.sdp;
        this.sdpMid = candidate.sdpMid;
        this.sdpMLineIndex = candidate.sdpMLineIndex;
    }

    // 🔹 Offer 또는 Answer 전송용 생성자
    public SignalingData(String type, String sdp) {
        this.type = type; // "offer" 또는 "answer"
        this.sdp = sdp;
    }
    public IceCandidate toIceCandidate() {
        return new IceCandidate(sdpMid, sdpMLineIndex, sdp);
    }

    /** 🔹 ICE 후보 여부 확인용 */
    public boolean hasCandidate() {
        return sdpMid != null && !sdpMid.isEmpty()
                && sdp != null && !sdp.isEmpty()
                && sdpMLineIndex >= 0;
    }

    /** 🔹 이 메시지가 offer에 대한 ICE 후보인지 확인 */
    public boolean isOffer() {
        return "offer".equals(type);
    }

}