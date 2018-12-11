package com.example.zjf.mqtt;

import android.app.Activity;
import android.app.Application;
import android.media.MediaPlayer;

import com.example.zjf.mqtt.listener.VideoCallCallBackListener;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.rtc.RtcImp;
import com.example.zjf.mqtt.rtc.RtcItf;
import com.example.zjf.mqtt.service.CoreApi;
import com.example.zjf.mqtt.util.PreferenceUtil;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    public static MyApplication application;
    public List<Activity> activityList = new LinkedList<Activity>();
    private MediaPlayer player;//铃声播放器
    private static RtcItf rtcItf;
    @Override
    public void onCreate() {
        super.onCreate();
        application = this;

        if (!CoreApi.isReady()) {
            CoreApi.startService(application);
        }


//        App.getInst().setContext(application);
        //setRtcItf(new RtcImp());
        //App.getInst().setVideoCallBack(new VideoCallCallBackListener(application));
    }

    /*public static void setRtcItf(RtcItf rtcItf) {
        application.rtcItf = rtcItf;
    }*/

    /*public static RtcItf getRtcItf() {
        return rtcItf;
    }*/

    public static MyApplication getApplication() {
        return application;
    }
    /**
     * 添加Activity到容器中
     * @param activity
     */
    public void addActivity(Activity activity) {
        for (Activity a : activityList) {
            if (a.equals(activity))
                return;
        }
        activityList.add(activity);
    }

    /**
     * 从容器中移出指定Activity
     * @param activity
     */
    public void removeActivity(Activity activity){
        activityList.remove(activity);
    }

    public void finishAllActivities(){
        for (Activity a : activityList) {
            a.finish();
        }
    }


    /**
     * 对外接口，释放铃声播放器实例
     */
    public void releaseMediaPlayerInstance() {
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
            player = null;
        }
    }
}
