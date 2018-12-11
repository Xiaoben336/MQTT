package com.example.zjf.mqtt.rtc;
import android.util.Log;

import com.example.zjf.mqtt.util.PreferenceUtil;

import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
public class RTCPeerConnection {
    private static final String TAG = "RTCPeerConnection";
    private final PCObserver pcObserver = new PCObserver();
    private final SDPObserver sdpObserver = new SDPObserver();
    private VideoRenderer.Callbacks remoteRender;
    private LinkedList<IceCandidate> queuedRemoteCandidates;
    private VideoTrack remoteVideoTrack;
    private AudioTrack remoteAudioTrack;
    private PeerConnection peerConnection;
    private boolean isCallout;
    private ScheduledExecutorService executor;
    private String uid;
    private boolean isError;
    private PeerConnectionEvents events;
    private SessionDescription localSdp; // either offer or answer SDP
    private MediaStream mediaStream;


    private DataChannel dataChannel;
    private boolean dataChannelEnabled = false;


    public RTCPeerConnection(String uid, VideoRenderer.Callbacks remoteRender, final MediaStream localmediaStream, final List<PeerConnection.IceServer> iceServers, boolean isCallout, final PeerConnection.IceTransportsType iceTransportsType, ScheduledExecutorService executor,PeerConnectionEvents events) {
        this.uid = uid;
        this.isCallout = isCallout;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.remoteRender = remoteRender;
        isError = false;
        this.events = events;
        createPeerConnectionInternal(localmediaStream, iceServers, iceTransportsType);
    }

    private void createPeerConnectionInternal(MediaStream localmediaStream,List<PeerConnection.IceServer> iceServers,PeerConnection.IceTransportsType iceTransportsType) {

        Log.d(TAG, "PCConstraints: " + RTCConnection.getInstance().getPcConstraints().toString());

        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;

        if(PreferenceUtil.getInstance().getString("BundlePolicy","0").equals("0")){
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.BALANCED;
        }else if(PreferenceUtil.getInstance().getString("BundlePolicy","0").equals("1")) {
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        }else if(PreferenceUtil.getInstance().getString("BundlePolicy","0").equals("2")){
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
        }
        if(PreferenceUtil.getInstance().getString("RtcpMuxPolicy","0").equals("0")){
            rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
        }else if(PreferenceUtil.getInstance().getString("RtcpMuxPolicy","0").equals("1")){
            rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        }

        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.iceTransportsType = iceTransportsType;

        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        Log.d(TAG, "createPeerConnection begin .");
        peerConnection = RTCConnection.getInstance().getFactory().createPeerConnection(rtcConfig, RTCConnection.getInstance().getPcConstraints(), pcObserver);
        RTCConnection.getInstance().setPeerConnection(peerConnection);
        Log.d(TAG, "createPeerConnection finish .");
        if (dataChannelEnabled) {
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = true;
            init.negotiated = false;
            init.maxRetransmits = -1;
            init.maxRetransmitTimeMs = -1;
            init.id = -1;
            init.protocol = "";
            dataChannel = peerConnection.createDataChannel("ApprtcDemo data", init);
        }

        Logging.enableTracing("logcat:", EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT));
        Logging.enableLogToDebugOutput(Logging.Severity.LS_ERROR);
        mediaStream = localmediaStream;
        peerConnection.addStream(mediaStream);

        Log.d(TAG, "Peer connection created.");
    }

    public void createOffer(final MediaConstraints sdpMediaConstraints) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(isCallout && !isError) {
                    peerConnection.createOffer(sdpObserver, sdpMediaConstraints);
                }
            }
        });
    }

    public void createAnswer(final MediaConstraints sdpMediaConstraints) {
        if(!isCallout && !isError) {
            peerConnection.createAnswer(sdpObserver, sdpMediaConstraints);
        }
    }

    public void addIceCandidate(final IceCandidate candidate) {
        if(!isError) {
            peerConnection.addIceCandidate(candidate);
        }
    }

    public void removeIceCandidates(final IceCandidate[] candidates) {
        if(!isError) {
            peerConnection.removeIceCandidates(candidates);
        }
    }

    private void reportError(final String errorMessage) {
        RTCConnection.getInstance().reportError(errorMessage);
    }
    public void setRemoteDescription(final SessionDescription sdpRemote) {
        Log.d(TAG, "setRemoteDescription:" + sdpRemote.type + " " + sdpRemote.description);
        if(!isError) {
            peerConnection.setRemoteDescription(sdpObserver, sdpRemote);
        }
    }

    public void dispose() {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (dataChannel != null) {
                    dataChannel.dispose();
                    dataChannel = null;
                }
                peerConnection.removeStream(mediaStream);
                peerConnection.dispose();
                events.onPeerConnectionClosed();
                isError = true;
                executor.shutdown();
                executor = null;
            }
        });
    }

    public void setAudioEnabled(final boolean enable) {
        if (remoteAudioTrack != null) {
            remoteAudioTrack.setEnabled(enable);
        }
    }
    public void setVideoEnabled(final boolean enable) {
        if (remoteVideoTrack != null) {
            remoteVideoTrack.setEnabled(enable);
        }
    }


    // Implementation detail: observe ICE & stream changes and react accordingly.
    private class PCObserver implements PeerConnection.Observer {
        @Override
        public void onIceCandidate(final IceCandidate candidate){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"onIceCandidate: ");
                    events.onIceCandidate(candidate);
                }
            });
        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"onIceCandidatesRemoved: ");
                    events.onIceCandidatesRemoved(candidates);
                }
            });
        }

        @Override
        public void onSignalingChange(
                PeerConnection.SignalingState newState) {
            Log.d(TAG,"SignalingState: " + newState);
        }

        @Override
        public void onIceConnectionChange(
                final PeerConnection.IceConnectionState newState) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"IceConnectionState: " + newState);
                    if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                        events.onIceConnected();
                    } else if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
                        events.onIceDisconnected();
                    } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                    }
                }
            });
        }

        @Override
        public void onIceGatheringChange(
                PeerConnection.IceGatheringState newState) {
            Log.d(TAG,"IceGatheringState: " + newState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            Log.d(TAG,"IceConnectionReceiving changed to " + receiving);
        }

        @Override
        public void onAddStream(final MediaStream stream){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"onAddStream " + peerConnection);
                    if (peerConnection == null || isError) {
                        return;
                    }
                    if (stream.audioTracks.size() > 1 || stream.videoTracks.size() > 1) {
                        Log.d(TAG,"Weird-looking stream: " + stream);
                        return;
                    }
                    if(stream.audioTracks.size() == 1){
                        remoteAudioTrack = stream.audioTracks.get(0);
                    }
                    if (stream.videoTracks.size() == 1) {
                        remoteVideoTrack = stream.videoTracks.get(0);
                        remoteVideoTrack.setEnabled(true);
                        remoteVideoTrack.addRenderer(new VideoRenderer(remoteRender));
                    }
                }
            });
        }

        @Override
        public void onRemoveStream(final MediaStream stream){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"onRemoveStream");
                    remoteAudioTrack = null;
                    remoteVideoTrack = null;
                }
            });
        }

        @Override
        public void onDataChannel(final DataChannel dc) {
            Log.d(TAG,"dc rev!");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG,"onRenegotiationNeeded!");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d(TAG,"onAddTrack!");
        }
    }

    private void drainCandidates() {
        queuedRemoteCandidates = RTCConnection.getInstance().getQueuedRemoteCandidates().get(uid);
        if (queuedRemoteCandidates != null) {
            Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
            for (IceCandidate candidate : queuedRemoteCandidates) {
                peerConnection.addIceCandidate(candidate);
            }
            queuedRemoteCandidates.clear();
            queuedRemoteCandidates = null;
        }
    }

    private class SDPObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            Log.d(TAG,"SDPObserver onCreateSuccess!");
            if (localSdp != null) {
                reportError("Multiple SDP create.");
                return;
            }
            localSdp = RTCConnection.getInstance().getLocalSdp(origSdp.type,origSdp.description);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection != null && !isError) {
                        Log.d(TAG, "Set local SDP from " + localSdp.type);
                        peerConnection.setLocalDescription(sdpObserver, localSdp);
                    }
                }
            });
        }

        @Override
        public void onCreateFailure(final String error) {
            Log.d(TAG,"SDPObserver onCreateFailure!");
            reportError("createSDP error: " + error);
        }

        @Override
        public void onSetFailure(final String error) {
            Log.d(TAG,"SDPObserver onSetFailure!");
            reportError("setSDP error: " + error);
        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG,"SDPObserver onSetSuccess!");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection == null || isError) {
                        return;
                    }
                    if (isCallout) {
                        if (peerConnection.getRemoteDescription() == null) {
                            Log.d(TAG, "Local SDP set succesfully");
                            events.onLocalDescription(localSdp);
                        } else {
                            Log.d(TAG, "Remote SDP set succesfully");
                            drainCandidates();
                        }
                    } else {
                        if (peerConnection.getLocalDescription() != null) {
                            Log.d(TAG, "Local SDP set succesfully");
                            events.onLocalDescription(localSdp);
                            drainCandidates();
                        } else {
                            Log.d(TAG, "Remote SDP set succesfully");
                        }
                    }
                }
            });
        }
    }
}
