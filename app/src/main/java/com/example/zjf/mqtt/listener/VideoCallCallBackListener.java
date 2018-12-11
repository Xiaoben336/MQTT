package com.example.zjf.mqtt.listener;

import android.content.Context;

import com.example.zjf.mqtt.activity.VideoCallActivity;
import com.example.zjf.mqtt.event.OnAcceptMessageEvent;
import com.example.zjf.mqtt.event.OnConnectMessageEvent;
import com.example.zjf.mqtt.event.OnInviteMessageEvent;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.util.CallUtils;

import org.greenrobot.eventbus.EventBus;

public class VideoCallCallBackListener implements VideoCallListener {
    private Context context;
    public VideoCallCallBackListener (Context context){
        this.context = context;
    }
    /**
     * 接到来电时回调
     * @param
     * @param
     */
    @Override
    public void onInvite(String var1, String var2) {
        CallUtils.getInst().setCallOutModel(false);
        MqttUtils.getInstance(context).subscribe(var1);
        VideoCallActivity.startActivity(context,var1,false);
        //EventBus.getDefault().post(new OnInviteMessageEvent("OnInvite"));
    }
    /**
     * 对方接受用户的视频电话的回调
     * @param
     */
    @Override
    public void onAccept(String var1) {
        EventBus.getDefault().post(new OnAcceptMessageEvent(var1));
    }
    /**
     * 对方挂断的回调
     * @param
     */
    @Override
    public void onBye(String var1) {

    }
    /**
     * 对方正忙的回调
     * @param
     */
    @Override
    public void onBusy(String var1) {

    }

    /**
     * 通话成功的回调
     * @param var1 topic
     */
    @Override
    public void onConnect(String var1) {
        EventBus.getDefault().post(new OnConnectMessageEvent(var1));
    }

    @Override
    public void onChannelClose(Long var1) {

    }
}
