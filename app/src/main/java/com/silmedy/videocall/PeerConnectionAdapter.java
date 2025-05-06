// PeerConnectionObserver.java
package com.silmedy.videocall;

import android.util.Log;

import org.webrtc.*;

/**
 * PeerConnection.Observer 기본 구현
 */
public abstract class PeerConnectionAdapter implements PeerConnection.Observer {
    @Override
    public void onSignalingChange(PeerConnection.SignalingState newState) {
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.d("WebRTCManager", "🛡 onIceConnectionChange: " + newState);
    }

    @Override
    public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
        Log.d("WebRTCManager", "⚙️ onConnectionChange: " + newState);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
    }

    @Override
    public void onAddStream(MediaStream stream) {
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
    }

    @Override
    public void onDataChannel(DataChannel dc) {
    }

    @Override
    public void onRenegotiationNeeded() {
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {
    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {
    }
}