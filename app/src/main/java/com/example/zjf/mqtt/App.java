package com.example.zjf.mqtt;

import android.content.Context;
import android.os.Looper;

import com.example.zjf.mqtt.listener.VideoCallListener;
import com.example.zjf.mqtt.rtc.RTCConnection;
import com.example.zjf.mqtt.rtc.RtcItf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    private static App inst;
    private Context context;
    private Map<String,Object> globalMap = new ConcurrentHashMap<>();
    private static RtcItf rtcItf;
    public static App getInst(){
        if (inst == null) {
            inst = new App();
        }
        return inst;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Map<String, Object> getGlobalMap() {
        return globalMap;
    }

    public void setGlobalMap(Map<String, Object> globalMap) {
        this.globalMap = globalMap;
    }

    public Looper getMainLooper(){return context.getMainLooper();}

    public void setPeerConnectionClient(RTCConnection instance){
        if (instance == null) {
            getGlobalMap().remove("RTCConnection");
        } else {
            getGlobalMap().put("RTCConnection",instance);
        }
    }

    public RTCConnection getPeerConnectionClient(){
        return (RTCConnection) getGlobalMap().get("RTCConnection");
    }

   public void setVideoCallBack(VideoCallListener listener){
        if (listener == null) {
            this.getGlobalMap().remove("videocalllistener");
        } else this.getGlobalMap().put("videocalllistener",listener);
   }

   public VideoCallListener getVideoCallBack(){
        VideoCallListener listener = (VideoCallListener) getGlobalMap().get("videocalllistener");
        if (listener == null) {
            listener = new VideoCallListener() {
                @Override
                public void onInvite(String var1, String var2) {

                }

                @Override
                public void onAccept(String var1) {

                }

                @Override
                public void onBye(String var1) {

                }

                @Override
                public void onBusy(String var1) {

                }

                @Override
                public void onConnect(String var1) {

                }

                @Override
                public void onChannelClose(Long var1) {

                }
            };
        }
        return listener;
   }
}
