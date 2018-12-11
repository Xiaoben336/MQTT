package com.example.zjf.mqtt.callback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.zjf.mqtt.event.MessageEvent;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.greenrobot.eventbus.EventBus;

public class SubscriberCallBackHandler implements IMqttActionListener {
    private static final String TAG = "SubscriberCallBack";
    private Context context;

    public SubscriberCallBackHandler(Context context){
        this.context = context;
    }
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        Log.d(TAG,"订阅成功");
        //EventBus.getDefault().post(new MessageEvent("订阅成功"));
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Log.d(TAG,"订阅失败");
        Toast.makeText(context,"订阅失败",Toast.LENGTH_SHORT).show();
    }
}
