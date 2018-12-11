package com.example.zjf.mqtt.callback;

import android.content.Context;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

public class PublishCallBackHandler implements IMqttActionListener {
    private static final String TAG = "PublishCallBackHandler";
    private Context context;

    public PublishCallBackHandler(Context context){
        this.context = context;
    }
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        Toast.makeText(context,"发布成功",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Toast.makeText(context,"发布失败",Toast.LENGTH_SHORT).show();
    }
}
