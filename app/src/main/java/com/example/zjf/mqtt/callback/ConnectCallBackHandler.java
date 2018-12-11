package com.example.zjf.mqtt.callback;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.zjf.mqtt.activity.HomeActivity;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

public class ConnectCallBackHandler implements IMqttActionListener {
    private static final String TAG = "ConnectCallBackHandler";
    private Context context;

    public ConnectCallBackHandler(Context context) {
        this.context = context;
    }
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        Log.d(TAG,"onSuccess");
        Toast.makeText(context,"连接成功",Toast.LENGTH_SHORT).show();
        //在这里开始跳转activity
        HomeActivity.startActivity(context);
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Log.d(TAG,"onFailure" + asyncActionToken.toString());
        Toast.makeText(context,"连接失败",Toast.LENGTH_SHORT).show();
    }
}
