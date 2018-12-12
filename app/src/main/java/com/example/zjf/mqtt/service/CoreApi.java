package com.example.zjf.mqtt.service;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.zjf.mqtt.App;
import com.example.zjf.mqtt.bean.IceServerInfo;
import com.example.zjf.mqtt.listener.VideoCallCallBackListener;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.notification.NotificationService;
import com.example.zjf.mqtt.notification.common.IceServer;
import com.example.zjf.mqtt.rtc.RtcImp;
import com.example.zjf.mqtt.rtc.RtcItf;
import com.example.zjf.mqtt.util.CallUtils;
import com.example.zjf.mqtt.util.PreferenceUtil;

import org.webrtc.SurfaceViewRenderer;

import java.util.LinkedList;
import java.util.UUID;

public class CoreApi extends Service {
    private static final String TAG = "CoreApi";
    public static CoreApi the_service_instance = null;
    public static Context ctx;
    private static RtcItf rtcItf;
    public static boolean isReady(){
        return (the_service_instance != null);
    }
    public static void startService(Context context){
        Intent intent = new Intent(context,CoreApi.class);
        Log.d(TAG,"服务开启");
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        the_service_instance = this;
        PreferenceUtil.initPreference(this,"mqtt");

        initCore(the_service_instance);
        App.getInst().setVideoCallBack(new VideoCallCallBackListener(the_service_instance));
    }

    private void initCore(Context context) {
        ctx = context;
        App.getInst().setContext(context);
        //webrtc
        setRtcItf(new RtcImp());
        //mqtt
        MqttUtils.getInstance(App.getInst().getContext());
        initUUID();
        NotificationService.getInstance(context);//初始化通知处理器
    }

    private void initUUID() {
        if (null == PreferenceUtil.getInstance().getString("devid","")){
            String uuid = UUID.randomUUID().toString();
            PreferenceUtil.getInstance().setString("devid",uuid);
        }
    }

    public static void setRtcItf(RtcItf rtcItf) {
        CoreApi.rtcItf = rtcItf;
    }

    public static RtcItf getRtcItf() {
        return rtcItf;
    }

    public static CoreApi getThe_service_instance() {
        return the_service_instance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    private static SurfaceViewRenderer remoteRenderer;

    public static void initCall(Activity context, int width, int height, SurfaceViewRenderer localRenderer){
        CallUtils.getInst().initCall(context,width,height,localRenderer);
    }

    public static void inviteCall(String topic,String friendClientId,SurfaceViewRenderer renderer){
        callpeer(topic,friendClientId,renderer);
    }

    /**
     *              呼叫方呼叫
     * @param topic
     * @param friendClientId
     * @param renderer
     */
    private static void callpeer(String topic,String friendClientId, SurfaceViewRenderer renderer) {
        CallUtils.getInst().setCallOutModel(true);
        CallUtils.getInst().setFriendClientId(friendClientId);

        IceServer iceServer = new IceServer("turn:39.108.166.113:34708","QMturnuser","QMturnpass");
        IceServerInfo info = new IceServerInfo();
        LinkedList<IceServer> iceServers = new LinkedList<>();
        iceServers.add(iceServer);
        info.setIceServers(iceServers);
        CallUtils.getInst().setIceServers(info.getIceServers());
        CallUtils.getInst().startCall(renderer,topic,friendClientId,true);
        remoteRenderer = renderer;
    }


    /**
     *              接听方接听电话
     * @param topic
     * @param friendClientId
     * @param renderer
     */
    public static void acceptCall(String topic,String friendClientId,SurfaceViewRenderer renderer){
        acceptcall(topic,friendClientId,renderer);
    }

    /**
     *              接听方接听电话
     * @param topic
     * @param friendClientId
     * @param renderer
     */
    private static void acceptcall(String topic,String friendClientId, SurfaceViewRenderer renderer) {
        CallUtils.getInst().startCall(renderer,topic,friendClientId,false);
    }

    /**
     * 呼叫方挂断电话
     */
    public static void byeAll(String topic){
        CallUtils.getInst().hangUp(topic,4);
    }
}
