package com.example.zjf.mqtt.notification;

import android.util.Log;

import com.example.zjf.mqtt.App;
import com.example.zjf.mqtt.bean.IceServerInfo;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.notification.common.IceServer;
import com.example.zjf.mqtt.util.CallUtils;
import com.example.zjf.mqtt.util.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.LinkedList;

public class WebRTCNotificationProcessImp implements NotificationProcessItf {
    private static final String TAG = "WebRTCNotificationProce";
    IceServerInfo info = null;
    public String toprint(LinkedList<IceServer> iceServers){
        Log.e(TAG,"toprint");
        try {
            StringBuffer str=new StringBuffer();
            str.append("[");
            Log.e(TAG,"iceservice="+iceServers.size());
            for(IceServer ice:iceServers){
                str.append(ice.getUri()+";");
            }
            str.append("]");
            return str.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    @Override
    public void process(String msg) {
        try {
            final WebRtcNotification notification = (WebRtcNotification) JsonUtils.jsonToBean(msg,WebRtcNotification.class);
            final JSONObject json = new JSONObject(notification.msg);
            final String type = json.optString("type");
            final String topic = notification.getTopic();
            final String sendClientId = notification.getSendClientId();
            CallUtils.getInst().setFriendClientId(sendClientId);
            Log.d(TAG,"type === " + type + "   topic === " + topic + "   sendClientId === " + sendClientId);
            if (type.equals("offer")) {
                Log.d(TAG,"=============type === offer============");
                if (CallUtils.getInst().isHangUp()) {
                    CallUtils.getInst().setHangUp(false);
                }

                if (topic == null) {
                    Log.d(TAG,"TOPIC为空");
                    return;
                } else {

                    if (notification.getIceServers() != null && notification.getIceServers().size() > 0) {
                        Log.d(TAG,"这里");
                        info = new IceServerInfo();
                        info.setIceServers(notification.getIceServers());
                    } else {
                        Object obj = App.getInst().getGlobalMap().get("IceServer");
                        if (obj != null) {
                            Log.d(TAG,"还是这里");
                            info = new IceServerInfo();
                            LinkedList<IceServer> iceServers = new LinkedList<>();
                            iceServers.add((IceServer)obj);
                            info.setIceServers(iceServers);
                        } else {
                            Log.d(TAG,"没有ICESERVER");
                        }
                    }
                    final String sdp = json.getString("sdp");
                    SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type),sdp);
                    if (info.getIceServers() != null) {
                        Log.d(TAG,"走这里?");
                        CallUtils.getInst().setSignalingParameters(sendClientId,info.getIceServers(),sessionDescription);
                    }

                    CallUtils.getInst().setCallOutModel(false);
                    if (sendClientId == CallUtils.getInst().getFriendClientId()){
                        Log.d(TAG,"走这里???");
                        CallUtils.getInst().onRemoteDescription(sendClientId,sessionDescription);
                    }
                }
            } else if (type.equals("answer")){
                Log.d(TAG,"=============type === answer============");
                if (CallUtils.getInst().isCallOutModel()) {
                    String sdp = json.getString("sdp");

                    CallUtils.getInst().initAudio();
                    SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type),sdp);
                    CallUtils.getInst().onRemoteDescription(sendClientId,sessionDescription);
                }
            } else if (type.equals("candidate")) {
                Log.d(TAG,"=============type === candidate============");
                String s = json.getString("id");
                IceCandidate iceCandidate = new IceCandidate(s,json.getInt("label"),json.getString("candidate"));
                CallUtils.getInst().onRemoteIceCandidate(sendClientId,iceCandidate);
            } else if (type.equals("bye")) {
                if (CallUtils.getInst().isHangUp()) {
                    return;
                }
                Log.d(TAG,"=============type === bye==========notification.getSendClientId()==" + notification.getSendClientId());
                CallUtils.getInst().onChannelClose(notification.getSendClientId(),4);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
