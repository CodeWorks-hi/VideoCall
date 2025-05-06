package com.silmedy.videocall;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.List;

public class WebRTCManager implements FirebaseSignalingClient.Callback {
    private static final String TAG = "WebRTCManager";

    private final Context context;
    private final EglBase eglBase;
    private final SurfaceViewRenderer remoteView;
    private final SurfaceViewRenderer localView;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private FirebaseSignalingClient signalingClient;
    private String roomId;

    public WebRTCManager(Context ctx, EglBase eglBase,
                         SurfaceViewRenderer remoteView,
                         SurfaceViewRenderer localView) {
        this.context = ctx.getApplicationContext();
        this.eglBase = eglBase;
        this.remoteView = remoteView;
        this.localView = localView;
        Log.d(TAG, "Constructor called");
        initFactory();
        initPeerConnection();
        initLocalMedia();
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
        Log.d(TAG, "setRoomId() called. roomId=" + roomId);
        if (signalingClient == null) {
            signalingClient = new FirebaseSignalingClient(roomId, this);
            Log.d(TAG, "FirebaseSignalingClient initialized");
        }
    }

    private void initFactory() {
        Log.d(TAG, "initFactory() start");
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions());
        AudioDeviceModule adm = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule();
        factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                        eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(
                        eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
        Log.d(TAG, "PeerConnectionFactory created");
    }

    private void initPeerConnection() {
        Log.d(TAG, "initPeerConnection() start");
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder(
                "stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder(
                        "turn:13.209.17.4:3478")
                .setUsername("testuser").setPassword("testpass")
                .createIceServer());
        PeerConnection.RTCConfiguration cfg =
                new PeerConnection.RTCConfiguration(iceServers);

        peerConnection = factory.createPeerConnection(cfg,
                new PeerConnectionAdapter() {
                    @Override
                    public void onIceCandidate(IceCandidate candidate) {
                        Log.d(TAG, "onIceCandidate() candidate=" + candidate.sdp);
                        signalingClient.sendIceCandidate(new SignalingData(candidate));
                    }

                    @Override
                    public void onTrack(RtpTransceiver transceiver) {
                        MediaStreamTrack track = transceiver.getReceiver().track();
                        if (track instanceof VideoTrack) {
                            VideoTrack vt = (VideoTrack) track;
                            remoteView.post(() -> vt.addSink(remoteView));
                        }
                    }

                    @Override
                    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
                        Log.d(TAG, "onIceConnectionChange() state=" + newState);
                        if (newState == PeerConnection.IceConnectionState.DISCONNECTED
                                || newState == PeerConnection.IceConnectionState.CLOSED
                                || newState == PeerConnection.IceConnectionState.FAILED) {
                            dispose();
                            if (context instanceof Activity) {
                                ((Activity) context).runOnUiThread(
                                        () -> ((Activity) context).finish()
                                );
                            }
                        }
                    }
                }
        );

        if (peerConnection == null) {
            Log.e(TAG, "Failed to create PeerConnection");
        } else {
            Log.d(TAG, "PeerConnection created");
        }
    }

    private void initLocalMedia() {
        Log.d(TAG, "initLocalMedia() start");
        VideoCapturer capturer = createCameraCapturer();
        SurfaceTextureHelper helper = SurfaceTextureHelper.create(
                "CaptureThread", eglBase.getEglBaseContext());
        VideoSource vs = factory.createVideoSource(false);
        capturer.initialize(helper, context, vs.getCapturerObserver());
        try {
            capturer.startCapture(640, 480, 30);
        } catch (Exception e) {
            Log.e(TAG, "startCapture() failed", e);
        }
        VideoTrack localVideo = factory.createVideoTrack("ARDAMSv0", vs);
        localVideo.addSink(localView);

        AudioSource as = factory.createAudioSource(new MediaConstraints());
        AudioTrack localAudio = factory.createAudioTrack("ARDAMSa0", as);
        peerConnection.addTrack(localVideo);
        peerConnection.addTrack(localAudio);
        Log.d(TAG, "Local tracks added");
    }

    private VideoCapturer createCameraCapturer() {
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        for (String name : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(name)) {
                VideoCapturer cap = enumerator.createCapturer(name, null);
                if (cap != null) return cap;
            }
        }
        for (String name : enumerator.getDeviceNames()) {
            VideoCapturer cap = enumerator.createCapturer(name, null);
            if (cap != null) return cap;
        }
        throw new RuntimeException("No camera available");
    }

    /**
     * Caller 역할: Offer 생성 후 전송
     */
    public void createOfferAndSend(String roomId) {
        this.roomId = roomId;
        // 신규 시그널링 클라이언트 초기화 (이미 init 되었다면 중복 무시)
        if (signalingClient == null) {
            signalingClient = new FirebaseSignalingClient(roomId, this);
        }
        peerConnection.createOffer(new SdpAdapter("createOffer") {
            @Override
            public void onCreateSuccess(SessionDescription offer) {
                peerConnection.setLocalDescription(
                        new SdpAdapter("setLocalOffer"), offer);
                signalingClient.sendOffer(offer.description);
            }
        }, new MediaConstraints());
    }

    /**
     * Callee 역할: Answer 생성 후 전송
     */
    public void createAnswerAndSend(String roomId) {
        this.roomId = roomId;
        if (signalingClient == null) {
            signalingClient = new FirebaseSignalingClient(roomId, this);
        }
        peerConnection.createAnswer(new SdpAdapter("createAnswer") {
            @Override
            public void onCreateSuccess(SessionDescription answer) {
                peerConnection.setLocalDescription(
                        new SdpAdapter("setLocalAnswer"), answer);
                signalingClient.sendAnswer(answer.description);
            }
        }, new MediaConstraints());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // FirebaseSignalingClient.Callback 구현
    // ────────────────────────────────────────────────────────────────────────────
    @Override
    public void onOfferReceived(String sdp) {
        SessionDescription offerDesc =
                new SessionDescription(SessionDescription.Type.OFFER, sdp);
        peerConnection.setRemoteDescription(
                new SdpAdapter("setRemoteOffer"), offerDesc);
        // Offer 받은 후 곧바로 Answer 생성/전송
        createAnswerAndSend(roomId);
    }

    @Override
    public void onAnswerReceived(String sdp) {
        SessionDescription answerDesc =
                new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        peerConnection.setRemoteDescription(
                new SdpAdapter("setRemoteAnswer"), answerDesc);
    }

    @Override
    public void onIceCandidateReceived(IceCandidate candidate) {
        peerConnection.addIceCandidate(candidate);
    }

    /**
     * 연결 종료 시 리소스 해제
     */
    public void dispose() {
        Log.d(TAG, "dispose() called");
        if (signalingClient != null) signalingClient.stop();
        if (peerConnection != null) peerConnection.dispose();
        if (factory != null) factory.dispose();
        eglBase.release();
    }
}