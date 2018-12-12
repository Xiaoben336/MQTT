package com.example.zjf.mqtt.rtc;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.zjf.mqtt.App;
import com.example.zjf.mqtt.MyApplication;
import com.example.zjf.mqtt.R;
import com.example.zjf.mqtt.activity.VideoCallActivity;
import com.example.zjf.mqtt.service.CoreApi;
import com.example.zjf.mqtt.util.CallUtils;
import com.example.zjf.mqtt.util.PreferenceUtil;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RTCConnection {
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    public static final String VIDEO_TRACK_TYPE = "video";
    private static final String TAG = "PCRTCClient";
    private static final String VIDEO_CODEC_VP8 = "VP8";
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String VIDEO_CODEC_H264 = "H264";
    private static final String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
    private static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    private static final String AUDIO_CODEC_OPUS = "opus";
    private static final String AUDIO_CODEC_ISAC = "ISAC";
    private static final String VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate";
    private static final String VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    private static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    private static final String VIDEO_H264_HIGH_PROFILE_FIELDTRIAL =
            "WebRTC-H264HighProfile/Enabled/";
    private static final String DISABLE_WEBRTC_AGC_FIELDTRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";
    private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static final String AUDIO_LEVEL_CONTROL_CONSTRAINT = "levelControl";
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
    private static final int HD_VIDEO_WIDTH = 1280;
    private static final int HD_VIDEO_HEIGHT = 720;
    private static final int BPS_IN_KBPS = 1000;

    private static RTCConnection instance = null;
    private ScheduledExecutorService executor;

    private PeerConnectionFactory factory;
    private Map<String, RTCPeerConnection> peerConnections;
    private Map<String, RTCPeerConnection> rmvpeerConnections;
    PeerConnectionFactory.Options options = null;
    private AudioSource audioSource;
    private VideoSource videoSource;
    private boolean videoCallEnabled;
    private boolean preferIsac;
    private String preferredVideoCodec;
    private boolean videoCapturerStopped;
    //    private Timer statsTimer;
    private VideoRenderer.Callbacks localRender;
    private MediaConstraints pcConstraints;
    private MediaConstraints audioConstraints;
    private MediaConstraints sdpMediaConstraints;
    private int videoWidth;
    private int videoHeight;
    private int videoFps;
    private PeerConnectionParameters peerConnectionParameters;
    private MediaStream mediaStream;
    private VideoCapturer videoCapturer;
    private boolean isError;

    // enableVideo is set to true if video should be rendered and sent.
    private boolean renderVideo;
    private VideoTrack localVideoTrack;
    // enableAudio is set to true if audio should be sent.
    private boolean enableAudio;
    private AudioTrack localAudioTrack;
    private Map<String,LinkedList<IceCandidate>> queuedRemoteCandidates;
    private boolean isRTCClosed;
    private boolean isLocalVideo = false;
    private RecordVideoFileRenderer localFileRender;


    private RTCConnection() {
        executor = Executors.newSingleThreadScheduledExecutor();
        peerConnections = new ConcurrentHashMap<String, RTCPeerConnection>();
        rmvpeerConnections = new ConcurrentHashMap<String, RTCPeerConnection>();
        isRTCClosed = false;
        this.isLocalVideo = false;
        localFileRender = null;
    }

    public static RTCConnection getInstance() {
        if(instance==null){
            instance = new RTCConnection();
        }
        return instance;
    }

    public void setPeerConnectionFactoryOptions(PeerConnectionFactory.Options options) {
        this.options = options;
    }
    public void createPeerConnectionFactory(
            final Context context,
            final VideoRenderer.Callbacks localRender,
            final EglBase.Context renderEGLContext,
            final PeerConnectionParameters peerConnectionParameters,final boolean isLocalVideo) {
        this.isLocalVideo = isLocalVideo;
        String saveRemoteVideoToFile = Environment.getExternalStorageDirectory() + "/"+"/tmp.y4m";
        try {
            localFileRender = new RecordVideoFileRenderer(
                    saveRemoteVideoToFile, peerConnectionParameters.videoWidth, peerConnectionParameters.videoHeight, CallUtils.getInst().getRootEglBase().getEglBaseContext());
        } catch (IOException e) {
            e.printStackTrace();

            this.isLocalVideo = false;
            localFileRender = null;
        }
        createPeerConnectionFactory(context,localRender,renderEGLContext,peerConnectionParameters);
    }
    public void createPeerConnectionFactory(
            final Context context,
            final VideoRenderer.Callbacks localRender,
            final EglBase.Context renderEGLContext,
            final PeerConnectionParameters peerConnectionParameters) {
        this.peerConnectionParameters = peerConnectionParameters;
        this.localRender = localRender;
        videoCallEnabled = peerConnectionParameters.videoCallEnabled;
        if(localRender==null)
            videoCallEnabled = false;
        // Reset variables to initial states.
        factory = null;
        preferIsac = false;
        videoCapturerStopped = false;
        isError = false;
        mediaStream = null;
        videoCapturer = null;
        renderVideo = true;
        localVideoTrack = null;
        enableAudio = true;
        localAudioTrack = null;
        this.videoWidth = peerConnectionParameters.videoWidth;
        this.videoHeight = peerConnectionParameters.videoHeight;
        this.videoFps = peerConnectionParameters.videoFps;
        statsTimer = new Timer();
        queuedRemoteCandidates = new ConcurrentHashMap<String,LinkedList<IceCandidate>>();
        if(isRTCClosed) return;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(isRTCClosed) return;
                try {
                    createMediaConstraintsInternal();
                    createPeerConnectionFactoryInternal(context, renderEGLContext);
                }
                catch (Exception e){
                    reportError("Failed to create peer connection: " + e.getMessage());
                    return;
                }
            }
        });
    }

    public void createPeerConnection(
            final String uid,
            final VideoRenderer.Callbacks remoteRender,
            final List<PeerConnection.IceServer> iceServers,
            final boolean isCallout,
            final PeerConnection.IceTransportsType iceTransportsType,
            final PeerConnectionEvents events) {
        Log.e(TAG,"createPeerConnection...");
        this.events=events;
        if (peerConnectionParameters == null) {
            Log.e(TAG, "Creating peer connection without initializing factory.");
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"create pe s");
                RTCPeerConnection pe=new RTCPeerConnection(uid, remoteRender, mediaStream, iceServers, isCallout, iceTransportsType, executor, events);
                Log.e(TAG,"pe=" + pe);
                peerConnections.put(uid,pe);
            }
        });
    }

    public void close() {
        isRTCClosed = true;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                closeInternal();
            }
        });
    }

    public boolean isVideoCallEnabled() {
        return videoCallEnabled;
    }

    private void createPeerConnectionFactoryInternal(Context context,final EglBase.Context renderEGLContext) {
        PeerConnectionFactory.initializeInternalTracer();
        if (peerConnectionParameters.tracing) {
            PeerConnectionFactory.startInternalTracingCapture(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                            + "webrtc-trace.txt");
        }
        Log.d(TAG, "Create peer connection factory. Use video: " +
                peerConnectionParameters.videoCallEnabled);

        isError = false;

        String fieldTrials = "";
        if (peerConnectionParameters.videoFlexfecEnabled) {
            fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL;
            Log.d(TAG, "Enable FlexFEC field trial.");
        }
        fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL;
        preferredVideoCodec = VIDEO_CODEC_VP8;
        if (videoCallEnabled && peerConnectionParameters.videoCodec != null) {
            switch (peerConnectionParameters.videoCodec) {
                case VIDEO_CODEC_VP8:
                    preferredVideoCodec = VIDEO_CODEC_VP8;
                    break;
                case VIDEO_CODEC_VP9:
                    preferredVideoCodec = VIDEO_CODEC_VP9;
                    break;
                case VIDEO_CODEC_H264_BASELINE:
                    preferredVideoCodec = VIDEO_CODEC_H264;
                    break;
                case VIDEO_CODEC_H264_HIGH:
                    // TODO(magjed): Strip High from SDP when selecting Baseline instead of using field trial.
                    fieldTrials += VIDEO_H264_HIGH_PROFILE_FIELDTRIAL;
                    preferredVideoCodec = VIDEO_CODEC_H264;
                    break;
                default:
                    preferredVideoCodec = VIDEO_CODEC_VP8;
            }
        }
        // Initialize field trials.
        Log.d(TAG, "Preferred video codec: " + preferredVideoCodec);
        PeerConnectionFactory.initializeFieldTrials(fieldTrials);

        preferIsac = false;
        if (peerConnectionParameters.audioCodec != null
                && peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC)) {
            preferIsac = true;
        }
        // Enable/disable OpenSL ES playback.
        if (!peerConnectionParameters.useOpenSLES) {
            Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */);
        } else {
            Log.d(TAG, "Allow OpenSL ES audio if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
        }

        if (peerConnectionParameters.disableBuiltInAEC) {
            Log.d(TAG, "Disable built-in AEC even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        } else {
            Log.d(TAG, "Enable built-in AEC if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
        }

        if (peerConnectionParameters.disableBuiltInAGC) {
            Log.d(TAG, "Disable built-in AGC even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
        } else {
            Log.d(TAG, "Enable built-in AGC if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(false);
        }

        if (peerConnectionParameters.disableBuiltInNS) {
            Log.d(TAG, "Disable built-in NS even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        } else {
            Log.d(TAG, "Enable built-in NS if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
        }

        WebRtcAudioRecord.setErrorCallback(new WebRtcAudioRecord.WebRtcAudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    WebRtcAudioRecord.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
                reportError(errorMessage);
            }
        });

        WebRtcAudioTrack.setErrorCallback(new WebRtcAudioTrack.WebRtcAudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(String errorMessage) {
                reportError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                reportError(errorMessage);
            }
        });
        PeerConnectionFactory.initializeAndroidGlobals(context, peerConnectionParameters.videoCodecHwAcceleration);
        if (options != null) {
            Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
        }
        factory = new PeerConnectionFactory(options);
        Log.d(TAG, "Peer connection factory created.");

        mediaStream = factory.createLocalMediaStream("ARDAMS");
        if (videoCallEnabled) {
            videoCapturer = createVideoCapturer();
            if (videoCapturer == null) {
                Log.e(TAG,"Failed to open camera");
            }
            else {
                mediaStream.addTrack(createVideoTrack(videoCapturer));
            }
        }
        mediaStream.addTrack(createAudioTrack());
        if (videoCallEnabled) {
            Log.d(TAG, "EGLContext: " + renderEGLContext);
            factory.setVideoHwAccelerationOptions(renderEGLContext, renderEGLContext);
        }
    }


    private void createMediaConstraintsInternal() {
        // Create peer connection constraints.
        pcConstraints = new MediaConstraints();
        // Enable DTLS for normal calls and disable for loopback calls.
        if (peerConnectionParameters.loopback) {
            pcConstraints.optional.add(
                    new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "false"));
        } else {
            pcConstraints.optional.add(
                    new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
        }
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair(
                "RtpDataChannels", "true"));
        if (videoCallEnabled) {
            videoWidth = peerConnectionParameters.videoWidth;
            videoHeight = peerConnectionParameters.videoHeight;
            videoFps = peerConnectionParameters.videoFps;

            if (videoWidth == 0 || videoHeight == 0) {
                videoWidth = HD_VIDEO_WIDTH;
                videoHeight = HD_VIDEO_HEIGHT;
            }

            if (videoFps == 0) {
                videoFps = 30;
            }
            Logging.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps);
        }

        audioConstraints = new MediaConstraints();

        if (peerConnectionParameters.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing");
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
        }
        // Create SDP constraints.
        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        if (videoCallEnabled || peerConnectionParameters.loopback) {
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo", "true"));
        } else {
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo", "false"));
        }
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer = null;
        /*String videoFileAsCamera = getIntent().getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
        if (videoFileAsCamera != null) {
            try {
                videoCapturer = new FileVideoCapturer(videoFileAsCamera);
            } catch (IOException e) {
                reportError("Failed to open video file for emulated camera");
                return null;
            }
        } else if (screencaptureEnabled) {
            if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
                reportError("User didn't give permission to capture the screen.");
                return null;
            }
            return new ScreenCapturerAndroid(
                    mediaProjectionPermissionResultData, new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    reportError("User revoked permission to capture the screen.");
                }
            });
        } else*/
        if (useCamera2()) {
            if (!captureToTexture()) {
                reportError(MyApplication.getApplication().getString(R.string.camera2_texture_only_error));
                return null;
            }
            Log.e(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(MyApplication.getApplication()));
        } else {
            Log.e(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(false));
        }
        if (videoCapturer == null) {
            return null;
        }
        return videoCapturer;
    }

    public static final String EXTRA_CAMERA2 = "org.appspot.apprtc.CAMERA2";

    private boolean useCamera2() {
        try {
            return Camera2Enumerator.isSupported(CoreApi.ctx) &&((Activity)CoreApi.ctx).getIntent().getBooleanExtra(EXTRA_CAMERA2, false);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean captureToTexture() {
        try {
            return Boolean.valueOf(PreferenceUtil.getInstance().getString("CAPTURETOTEXTURE", "true"));
        } catch (Exception e) {
            return false;
        }
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private Timer statsTimer;
    private void closeInternal() {
//        if (factory != null && peerConnectionParameters.aecDump) {
//            factory.stopAecDump();
//        }
        Log.d(TAG, "Closing peer connection.");
        statsTimer.cancel();
        Set<String> keys = peerConnections.keySet();
        for (String id : keys) {
            RTCPeerConnection pc = peerConnections.get(id);

            if (pc != null) {
                pc.dispose();
            }
        }
        peerConnections.clear();
        keys = rmvpeerConnections.keySet();
        for (String id : keys) {
            RTCPeerConnection pc = rmvpeerConnections.get(id);

            if (pc != null) {
                pc.dispose();
            }
        }
        rmvpeerConnections.clear();
        mediaStream.dispose();
        keys = queuedRemoteCandidates.keySet();
        for(String id : keys){
            LinkedList l = queuedRemoteCandidates.get(id);
            l.clear();
        }
        queuedRemoteCandidates.clear();

        Log.d(TAG, "Closing audio source.");
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        Log.d(TAG, "Stopping capture.");
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            videoCapturerStopped = true;
            videoCapturer.dispose();
            videoCapturer = null;
        }
        Log.d(TAG, "Closing video source.");
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        Log.d(TAG, "Closing peer connection factory.");
        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        if (localFileRender != null) {
            localFileRender.release();
            localFileRender = null;
        }
        if(executor!=null){
            executor.shutdownNow();
            executor = null;
        }
        options = null;
        Log.d(TAG, "Closing peer connection done.");
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
        App.getInst().setPeerConnectionClient(null);
        instance = null;
    }


    public PeerConnectionFactory getFactory() {
        return factory;
    }

    public MediaConstraints getAudioConstraints() {
        return audioConstraints;
    }

    public MediaConstraints getPcConstraints() {
        return pcConstraints;
    }

    private PeerConnection peerConnection;
    private PeerConnectionEvents events;

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }
    int timenum=1;
    public void getStats() {
        try {
            if (peerConnection == null || isError) {
                return;
            }
            /*if(CallUtils.getInst().isThreePartyCall()){
                NetWorkSpeedUtils netWorkSpeedUtils=new NetWorkSpeedUtils(CallUtils.getInst().getmContext(),CallUtils.getInst().getMonitorHandler());
                String netSpeed=netWorkSpeedUtils.showNetSpeed();
                if(PreferenceUtil.getInstance().getString("UploadMonitor","0").equals("1")) {
                    if (timenum >= 5) {
                        UploadComonMethod.reportPerformance(CallUtils.getInst().getGroupId()
                                , HostInterfaceManager.getHostApplicationItf().get_me().getId() + ""
                                , CallUtils.getInst().getPeer_uid() + ""
                                , ""
                                , ""
                                , ""
                                , netSpeed);
                    }
                    if (timenum == 5) {
                        timenum = 1;
                    } else {
                        timenum++;
                    }
                }
            }else {*/
                boolean success = peerConnection.getStats(new StatsObserver() {
                    @Override
                    public void onComplete(final StatsReport[] reports) {
                        events.onPeerConnectionStatsReady(reports);
                    }
                }, null);
                if (!success) {
                    Log.e(TAG, "getStats() returns false!");
                }
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getBaseStats() {
        if (peerConnection == null || isError) {
            return;
        }
        boolean success = peerConnection.getStats(new StatsObserver() {
            @Override
            public void onComplete(final StatsReport[] reports) {
                //HostInterfaceManager.getHostApplicationItf().getGlobalMap().put("baselineReport",reports);
            }
        }, null);
        if (!success) {
            Log.e(TAG, "getStats() returns false!");
        }
    }

    public void enableStatsEvents(boolean enable, int periodMs) {
        if (enable) {
            try {
                statsTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                getStats();
                            }
                        });
                    }
                }, 0, periodMs);
            } catch (Exception e) {
                Log.e(TAG, "Can not schedule statistics timer", e);
            }
        } else {
            statsTimer.cancel();
        }
    }

    public void createOffer(final String uid) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnections == null)
                    return;
                RTCPeerConnection pc = peerConnections.get(uid);
                Log.e(TAG,"pc="+pc);
                if (pc != null) {
                    Log.d(TAG, "PC Create OFFER");
                    pc.createOffer(sdpMediaConstraints);
                }
            }
        });
    }

    public void createAnswer(final String uid) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnections == null)
                    return;
                RTCPeerConnection pc = peerConnections.get((String) uid);
                if (pc != null ) {
                    Log.d(TAG, "PC create ANSWER");
                    pc.createAnswer(sdpMediaConstraints);
                }
            }
        });
    }

    public void addRemoteIceCandidate(final String uid, final IceCandidate candidate) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnections == null)
                    return;
                if(tobeCloseUids!=null&&tobeCloseUids.size()>0){
                    for(String l:tobeCloseUids){
                        if(l == uid){
                            return;
                        }
                    }
                }
                RTCPeerConnection pc = peerConnections.get(uid);
                if (pc != null) {
                    Log.d(TAG, "Add Candidate");
                    pc.addIceCandidate(candidate);
                }
                else{
                    if(queuedRemoteCandidates.get(uid)==null) {
                        queuedRemoteCandidates.put(uid,new LinkedList<IceCandidate>());
                    }
                    queuedRemoteCandidates.get(uid).add(candidate);
                }
            }
        });
    }

    public Map<String, LinkedList<IceCandidate>> getQueuedRemoteCandidates() {
        return queuedRemoteCandidates;
    }

    public void removeRemoteIceCandidates(final long uid, final IceCandidate[] candidates) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnections == null)
                    return;
                final RTCPeerConnection pc = peerConnections.get(uid);
                if (pc == null) {
                    return;
                }
                // Drain the queued remote candidates if there is any so that
                // they are processed in the proper order.
                pc.removeIceCandidates(candidates);
            }
        });
    }

    public void setRemoteDescription(final String uid, final SessionDescription sdp) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnections == null)
                    return;
                RTCPeerConnection pc = peerConnections.get((String) uid);
                if (pc == null) {
                    return;
                }
                String sdpDescription = sdp.description;
                if (preferIsac) {
                    sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
                }
                if (videoCallEnabled) {
                    sdpDescription = preferCodec(sdpDescription, preferredVideoCodec, false);
                }
                if (videoCallEnabled && peerConnectionParameters.videoMaxBitrate > 0) {
                    sdpDescription = setStartBitrate(VIDEO_CODEC_VP8, true,
                            sdpDescription, peerConnectionParameters.videoMaxBitrate);
                    sdpDescription = setStartBitrate(VIDEO_CODEC_VP9, true,
                            sdpDescription, peerConnectionParameters.videoMaxBitrate);
                    sdpDescription = setStartBitrate(VIDEO_CODEC_H264, true,
                            sdpDescription, peerConnectionParameters.videoMaxBitrate);
                }
                if (peerConnectionParameters.audioStartBitrate > 0) {
                    sdpDescription = setStartBitrate(AUDIO_CODEC_OPUS, false,
                            sdpDescription, peerConnectionParameters.audioStartBitrate);
                }
                Log.d(TAG, "Set remote SDP.");
                SessionDescription sdpRemote = new SessionDescription(
                        sdp.type, sdpDescription);
                pc.setRemoteDescription(sdpRemote);
            }
        });
    }

    public void stopVideoSource() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (videoCapturer != null && !videoCapturerStopped) {
                    Log.d(TAG, "Stop video source.");
                    try {
                        videoCapturer.stopCapture();
                    } catch (InterruptedException e) {
                    }
                    videoCapturerStopped = true;
                }
            }
        });
    }

    public void startVideoSource() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (videoCapturer != null && videoCapturerStopped) {
                    Log.d(TAG, "Restart video source.");
                    videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
                    videoCapturerStopped = false;
                }
            }
        });
    }


    private AudioTrack createAudioTrack() {
        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(enableAudio);
        return localAudioTrack;
    }

    private VideoTrack createVideoTrack(VideoCapturer capturer) {
        videoSource = factory.createVideoSource(capturer);
        capturer.startCapture(videoWidth, videoHeight, videoFps);

        localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(renderVideo);
        localVideoTrack.addRenderer(new VideoRenderer(localRender));
//        localVideoTrack.addRenderer(new VideoRenderer(new FakeRenderer()));
        if(isLocalVideo){
            localVideoTrack.addRenderer(new VideoRenderer(localFileRender));
        }
        return localVideoTrack;
    }

    private static String setStartBitrate(String codec, boolean isVideoCodec,
                                          String sdpDescription, int bitrateKbps) {
        String[] lines = sdpDescription.split("\r\n");
        int rtpmapLineIndex = -1;
        boolean sdpFormatUpdated = false;
        String codecRtpMap = null;
        // Search for codec rtpmap in format
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                rtpmapLineIndex = i;
                break;
            }
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec + " codec");
            return sdpDescription;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap
                + " at " + lines[rtpmapLineIndex]);

        // Check if a=fmtp string already exist in remote SDP for this codec and
        // update it with new bitrate parameter.
        regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
        codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                Log.d(TAG, "Found " + codec + " " + lines[i]);
                if (isVideoCodec) {
                    lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE
                            + "=" + bitrateKbps;
                } else {
                    lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE
                            + "=" + (bitrateKbps * 1000);
                }
                Log.d(TAG, "Update remote SDP line: " + lines[i]);
                sdpFormatUpdated = true;
                break;
            }
        }

        StringBuilder newSdpDescription = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            newSdpDescription.append(lines[i]).append("\r\n");
            // Append new a=fmtp line if no such line exist for a codec.
            if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                String bitrateSet;
                if (isVideoCodec) {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " "
                            + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
                } else {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " "
                            + AUDIO_CODEC_PARAM_BITRATE + "=" + (bitrateKbps * 1000);
                }
                Log.d(TAG, "Add remote SDP line: " + bitrateSet);
                newSdpDescription.append(bitrateSet).append("\r\n");
            }

        }
        return newSdpDescription.toString();
    }


    private static String preferCodec(
            String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        String mediaDescription = "m=video ";
        if (isAudio) {
            mediaDescription = "m=audio ";
        }
        for (int i = 0; (i < lines.length)
                && (mLineIndex == -1 || codecRtpMap == null); i++) {
            if (lines[i].startsWith(mediaDescription)) {
                mLineIndex = i;
                continue;
            }
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
            Log.w(TAG, "No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec);
            return sdpDescription;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at "
                + lines[mLineIndex]);
        String[] origMLineParts = lines[mLineIndex].split(" ");
        if (origMLineParts.length > 3) {
            StringBuilder newMLine = new StringBuilder();
            int origPartIndex = 0;
            // Format is: m=<media> <port> <proto> <fmt> ...
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(codecRtpMap);
            for (; origPartIndex < origMLineParts.length; origPartIndex++) {
                if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
                    newMLine.append(" ").append(origMLineParts[origPartIndex]);
                }
            }
            lines[mLineIndex] = newMLine.toString();
            Log.d(TAG, "Change media description: " + lines[mLineIndex]);
        } else {
            Log.e(TAG, "Wrong SDP media description format: " + lines[mLineIndex]);
        }
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }


    public void setVideoEnabled(final boolean enable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                renderVideo = enable;
                if (localVideoTrack != null) {
                    localVideoTrack.setEnabled(renderVideo);
                }

                /*Set<Long> keys = peerConnections.keySet();
                for (Long id : keys) {
                    RTCPeerConnection pc = peerConnections.get(id);

                    if (pc != null) {
                        pc.setVideoEnabled(renderVideo);
                    }
                }*/
            }
        });
    }
    private void switchCameraInternal() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            if (!videoCallEnabled || isError || videoCapturer == null) {
                Log.e(TAG, "Failed to switch camera. Video: " + videoCallEnabled + ". Error : " + isError);
                return; // No video is sent or only one camera is available or error happened.
            }
            Log.d(TAG, "Switch camera");
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        } else {
            Log.d(TAG, "Will not switch camera, video caputurer is not a camera");
        }
    }

    public void switchCamera() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                switchCameraInternal();
            }
        });
    }

    public void changeCaptureFormat(final int width, final int height, final int framerate) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                changeCaptureFormatInternal(width, height, framerate);
            }
        });
    }

    private void changeCaptureFormatInternal(int width, int height, int framerate) {
        if (!videoCallEnabled || isError || videoCapturer == null) {
            Log.e(TAG,
                    "Failed to change capture format. Video: " + videoCallEnabled + ". Error : " + isError);
            return;
        }
        Log.d(TAG, "changeCaptureFormat: " + width + "x" + height + "@" + framerate);
        videoSource.adaptOutputFormat(width, height, framerate);
    }

    public void reportError(final String errorMessage) {
        Log.e(TAG, "Peerconnection error: " + errorMessage);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    Log.d(TAG,"reportError : " + errorMessage);
                    isError = true;
                }
            }
        });
    }
    public SessionDescription getLocalSdp(SessionDescription.Type type,String description) {

        String sdpDescription = description;
        if (preferIsac) {
            sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
        }
        if (videoCallEnabled) {
            sdpDescription = preferCodec(sdpDescription, preferredVideoCodec, false);
        }
        return new SessionDescription(type, sdpDescription);
    }
    private List<String> tobeCloseUids=new ArrayList<>();
    public void close(final String uid) {
        if(tobeCloseUids==null)tobeCloseUids=new ArrayList<>();
        if(!tobeCloseUids.contains(uid)) {
            tobeCloseUids.add(uid);
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RTCPeerConnection pc = peerConnections.get(uid);

                if (pc != null) {
//                    pc.dispose();
                    peerConnections.remove(uid);
                    RTCPeerConnection pcold = rmvpeerConnections.get(uid);
                    if(pcold!=null){
                        pcold.dispose();
                        rmvpeerConnections.remove(uid);
                    }
                    rmvpeerConnections.put(uid,pc);
                    tobeCloseUids.remove(uid);
                }
            }
        });
    }
}
