package com.example.zjf.mqtt.util;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import com.example.zjf.mqtt.App;
import com.example.zjf.mqtt.MyApplication;
import com.example.zjf.mqtt.notification.NotificationTypeDef;
import com.example.zjf.mqtt.notification.WebRtcNotification;
import com.example.zjf.mqtt.notification.common.IceServer;
import com.example.zjf.mqtt.rtc.AppRTCAudioManager;
import com.example.zjf.mqtt.rtc.PeerConnectionEvents;
import com.example.zjf.mqtt.rtc.PeerConnectionParameters;
import com.example.zjf.mqtt.rtc.RtcItf;
import com.example.zjf.mqtt.service.CoreApi;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CallUtils {
    private static final String TAG = "CallUtils";
    private static CallUtils inst;

    public CallUtils() {}
    public static CallUtils getInst() {
        if (inst == null) {
            inst = new CallUtils();
        }
        return inst;
    }

    private RtcItf peerConnectionClient;
    private PeerConnectionParameters peerConnectionParameters;
    private EglBase rootEglBase;
    private Activity context;
    private boolean isPeerConnectionCreate = false;

    private boolean isSending;//是否在发送？？
    private boolean isHangUp ;//是否挂断
    private boolean isCallOutModel = false;//是否是呼叫模式
    public SessionDescription localSdp = null;//本地SDP
    private List<IceCandidate> localCandidateList = null;//本地ICE候选
    private LinkedList<IceServer> iceServers;//ICE Server

    private AppRTCAudioManager audioManager = null;
    private AudioManager am = null;

    private boolean inVideoCall = false;//是否在进行视频通话
    private boolean isConnected;//是否连接到转发服务器
    private String friendClientId;//以呼叫方来说，接听方的ID，以接听方来说，呼叫方的ID
    private String sendClientId;

    //存储保存下来的所有SDP
    public Map<String,SessionDescription> sdpMap = new HashMap<String, SessionDescription>();
    //存储保存下来的ICECandidate
    public Map<String,List<IceCandidate>> iceCandidateMap;
    public void setSignalingParameters(String uid, LinkedList<IceServer> iceServers, SessionDescription sdp) {
        this.iceServers = iceServers;
        if (sdpMap.get(uid) == null) {
            sdpMap.put(uid, sdp);
            Log.d(TAG,"setSignalingParameters === sendClientId === " + uid + "   PUT了");
        }
    }

    //视频接通时间
    private long startTimeMillis = 0;
    private long callStartedTimeMs = 0;

    public boolean isInVideoCall() {
        return inVideoCall;
    }

    public void setInVideoCall(boolean inVideoCall) {
        this.inVideoCall = inVideoCall;
    }

    public String getFriendClientId() {
        return friendClientId;
    }

    public void setFriendClientId(String friendClientId) {
        this.friendClientId = friendClientId;
    }

    public void setSendClientId(String sendClientId) {
        this.sendClientId = sendClientId;
    }

    public String getSendClientId() {
        return sendClientId;
    }

    /**
     * 是否开启扬声器
     * @param b
     */
    public void setSpeakerOn(boolean b){
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(b);
        }
    }

    public EglBase getRootEglBase() {
        return rootEglBase;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isCallOutModel() {
        return isCallOutModel;
    }

    public void setCallOutModel(boolean callOutModel) {
        isCallOutModel = callOutModel;
    }

    public LinkedList<IceServer> getIceServers() {
        return iceServers;
    }

    public void setIceServers(LinkedList<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public boolean isHangUp() {
        if (isHangUp) {
            Log.d(TAG,"已经挂断了");
        }
        return isHangUp;
    }

    public void setHangUp(boolean hangUp) {
        isHangUp = hangUp;

        if (!isHangUp) {

        } else {

        }
    }

    /**
     * 音频相关初始化
     */
    public void initAudio(){
        MyApplication.getApplication().releaseMediaPlayerInstance();
        audioManager = AppRTCAudioManager.create(context);
        audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(AppRTCAudioManager.AudioDevice selectedAudioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {

            }
        });
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void initCall(Activity context, int width, int height, SurfaceViewRenderer localRenderer){
        setHangUp(false);
        peerConnectionParameters =
                new PeerConnectionParameters(
                        true,
                        false,
                        false,
                        width,
                        height,
                        13,
                        0,
                        "H264",
                        true,
                        false,
                        0,
                        "opus",
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        true);

        peerConnectionClient = CoreApi.getRtcItf();
        rootEglBase = EglBase.create();
        peerConnectionClient.initPeerConnection();

        if (localRenderer != null) {
            localRenderer.init(getRootEglBase().getEglBaseContext(),null);
            localRenderer.setEnableHardwareScaler(true);
        }

        peerConnectionClient.createPeerConnectionFactory(context,localRenderer,rootEglBase.getEglBaseContext(),peerConnectionParameters);

        this.context = context;

        isPeerConnectionCreate = true;
    }

    public void startCall(final SurfaceViewRenderer renderer,final String topic,final String friendClientId,final boolean isCallOut){
        Log.d(TAG,"进入startCall");
        isSending = true;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (renderer != null) {
                        Log.d(TAG,"renderer != null");
                        renderer.init(CallUtils.getInst().getRootEglBase().getEglBaseContext(),null);
                        renderer.setEnableHardwareScaler(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                onConnectedToRoomInternel(renderer,topic,friendClientId,isCallOut);
            }
        });
    }

    private void onConnectedToRoomInternel(SurfaceViewRenderer renderer, final String topic,final String friendClientId,final boolean isCallOut) {
        Log.d(TAG,"onConnectedToRoomInternel" + "===topic===" + topic + "sendClientId === " + getFriendClientId());
        if (isHangUp()) {
            Log.d(TAG,"挂断了");
            return;
        }

        PeerConnectionEvents events = new PeerConnectionEvents() {
            String f = friendClientId;
            @Override
            public void onLocalDescription(final Object var1) {
                JSONObject json = new JSONObject();
                jsonPut(json,"sdp",((SessionDescription)var1).description);
                Log.e(TAG,"sendSdp == " + json.toString());
                if (isHangUp()) return;

                final long delta = System.currentTimeMillis() - callStartedTimeMs;

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Sending " + ((SessionDescription) var1).type + ", delay=" + delta + "ms");
                        if (isHangUp()) return;

                        if (isCallOut) {
                            if (isHangUp()) {
                                Log.e(TAG,"挂断002");
                            }
                            localSdp = (SessionDescription) var1;
                            sendOfferSdp(topic,(SessionDescription) var1);
                        } else {
                            if (isHangUp()) return;
                            sendAnswerSdp(topic,(SessionDescription) var1);
                        }
                    }
                });
            }

            @Override
            public void onIceCandidate(final Object var1) {
                if (isHangUp()) return;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (localCandidateList == null) {
                            localCandidateList = new ArrayList<>();
                        }
                        localCandidateList.add((IceCandidate) var1);
                        sendLocalIceCandidate(topic,(IceCandidate) var1);
                    }
                });
            }

            @Override
            public void onIceCandidatesRemoved(Object[] var1) {
                Log.d(TAG,"onIceCandidatesRemoved");
            }

            @Override
            public void onIceConnected() {
                if (isHangUp()) return;
                final long delta = System.currentTimeMillis() - callStartedTimeMs;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "ICE connected, delay=" + delta + "ms");
                        localCandidateList = null;
                        localSdp = null;
                        App.getInst().getVideoCallBack().onConnect(friendClientId);
                        setConnected(true);//isConnected = true
                    }
                });
            }

            @Override
            public void onIceDisconnected() {
                if (isHangUp()) return;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"onIceDisconnected");
                    }
                });
            }

            @Override
            public void onPeerConnectionClosed() {
                if (isHangUp()) return;
                Log.d(TAG,"onPeerConnectionClosed");
            }

            @Override
            public void onPeerConnectionStatsReady(Object[] var1) {
                if (isHangUp()) return;
                Log.d(TAG,"onPeerConnectionStatsReady");
            }

            @Override
            public void onPeerConnectionError(final String var1) {
                if (isHangUp()) return;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"onPeerConnectionError ：" + var1);
                    }
                });
            }
        };

        peerConnectionClient.createPeerConnection(friendClientId,renderer,iceServers, PeerConnection.IceTransportsType.RELAY,isCallOut,events);

        if (isHangUp()) return;

        if (isCallOut) {
            peerConnectionClient.createOffer(friendClientId);
            Log.d(TAG,"CREATING OFFER END");
        }else {
            Log.d(TAG,"CREATING ANSWER START");
            if (sdpMap.get(getFriendClientId()) != null) {
                Log.d(TAG,"sdpMap不为空" + sdpMap.get(getFriendClientId()).type  + sdpMap.get(getFriendClientId()).description);
                peerConnectionClient.setRemoteDescription(friendClientId,sdpMap.get(friendClientId));
                peerConnectionClient.createAnswer(friendClientId);
                if (iceCandidateMap != null) {
                    List<IceCandidate> iceCandidates = iceCandidateMap.get(getFriendClientId());

                    if (iceCandidates != null) {
                        for (IceCandidate iceCandidate : iceCandidates) {
                           peerConnectionClient.addRemoteIceCandidate(friendClientId,iceCandidate);
                        }
                        iceCandidates.clear();
                        iceCandidateMap.remove(friendClientId);
                    }
                } else {
                    Log.d(TAG,"iceCandidateMap为空");
                }
            } else {
                Log.d(TAG,"sdpMap为空");
            }
            Log.d(TAG,"CREATING ANSWER END");
        }
    }


    public static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *         发送OfferSdp
     * @param topic     发布的主题，即ClientId
     * @param sdp       本地OfferSdp
     */
    private void sendOfferSdp(final String topic/*,final String friendClientId*/,final SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");

        WebRtcNotification notification = new WebRtcNotification();
        notification.setNotify_type(NotificationTypeDef.C2C_NOTIFY_TYPE_WEBRTC);
        notification.setMsg(json.toString());
        notification.setTopic(topic);
        notification.setSendClientId(getSendClientId());
        if (iceServers != null) {
            Log.d(TAG,"offer iceServers != null");
            notification.setIceServers(iceServers);
        }
        C2CMessageUtils.sendMessageByTopic(topic,notification);
        isSending = false;
        Log.d(TAG, "sendSdpOffer:"  + json.toString());
    }

    /**
     *          发送AnswerSdp
     * @param topic     发布的主题，即ClientId
     * @param sdp      本地AnswerSdp
     */
    private void sendAnswerSdp(String topic,SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");

        WebRtcNotification notification = new WebRtcNotification();
        notification.setNotify_type(NotificationTypeDef.C2C_NOTIFY_TYPE_WEBRTC);
        notification.setMsg(json.toString());
        notification.setTopic(topic);
        notification.setSendClientId(getSendClientId());

        if (iceServers != null) {
            Log.d(TAG,"answer iceServers != null");
            notification.setIceServers(iceServers);
        }
        C2CMessageUtils.sendMessageByTopic(topic,notification);
        isSending = false;
        Log.d(TAG, "sendAnswerSdp:"  + json.toString());
    }

    /**
     *              发送本地ICECandidate
     * @param topic
     * @param var1
     */
    private void sendLocalIceCandidate(String topic,IceCandidate var1) {
        JSONObject json = new JSONObject();
        jsonPut(json,"type","candidate");
        jsonPut(json,"label",var1.sdpMLineIndex);
        jsonPut(json,"id",var1.sdpMid);
        jsonPut(json,"candidate",var1.sdp);
        WebRtcNotification notification = new WebRtcNotification();
        notification.setTopic(topic);
        notification.setNotify_type(NotificationTypeDef.C2C_NOTIFY_TYPE_WEBRTC);
        notification.setMsg(json.toString());
        notification.setSendClientId(getSendClientId());
        C2CMessageUtils.sendMessageByTopic(topic,notification);
        Log.d(TAG,"sendLocalIceCandidate === " + json.toString());
    }
    /**
     *          保存远端SDP并弹出接听页面
     * @param sendClientId
     * @param sdp
     */
    public void onRemoteDescription(final String sendClientId,final SessionDescription sdp){
        new Handler(MyApplication.getApplication().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"REMOTE SDP : " + sendClientId + " : " + sdp.type);
                if (isHangUp()) return;
                if (sdp.type == SessionDescription.Type.ANSWER) {
                    App.getInst().getVideoCallBack().onAccept(sendClientId);
                    if (peerConnectionClient == null) return;
                    peerConnectionClient.setRemoteDescription(sendClientId,sdp);
                } else if (sdp.type == SessionDescription.Type.OFFER) {
                    App.getInst().getVideoCallBack().onInvite(sendClientId,"");
                }
            }
        });
    }

    /**
     *              保存远端IceCandidate
     * @param clientId
     * @param iceCandidate
     */
    public void onRemoteIceCandidate(final String clientId, final IceCandidate iceCandidate) {
        new Handler(MyApplication.application.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (!isPeerConnectionCreate) {
                    if (iceCandidateMap == null) {
                        iceCandidateMap = new ConcurrentHashMap<String, List<IceCandidate>>();
                    }
                    List<IceCandidate> iceCandidates = iceCandidateMap.get(clientId);
                    if (iceCandidates == null) {
                        iceCandidates = new LinkedList<>();
                        iceCandidateMap.put(clientId,iceCandidates);
                    }
                    iceCandidates.add(iceCandidate);
                    return;
                }
                Log.d(TAG,"added RemoteIceCandidate === " + clientId);
                if (peerConnectionClient != null) {
                    peerConnectionClient.addRemoteIceCandidate(clientId,iceCandidate);
                }
            }
        });
    }
}