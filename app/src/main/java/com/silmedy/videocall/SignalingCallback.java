package com.silmedy.videocall;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public interface SignalingCallback {
    void onAnswerReceived(SessionDescription sdp);
    void onIceCandidateReceived(IceCandidate candidate);
}