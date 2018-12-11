package com.example.zjf.mqtt.callback;

import android.content.Context;
import android.util.Log;

import com.example.zjf.mqtt.event.MessageEvent;
import com.example.zjf.mqtt.notification.NotificationService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

public class MqttCallbackHandler implements MqttCallback {
    private static final String TAG = "MqttCallbackHandler";
    private Context context;
    private String strClientID;

    public MqttCallbackHandler(Context context,String strClientID){
        this.context = context;
        this.strClientID = strClientID;
    }
    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG,"connectionLost");
    }

    /**
     *
     * @param topic         主题
     * @param message       消息内容
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d(TAG,"messageArrived/topic === " + topic);
        Log.d(TAG,"messageArrived/message === " + new String(message.getPayload()));
        String s = new String(message.getPayload());
        NotificationService.getInstance(context).processMessage(s);
       // EventBus.getDefault().post(new MessageEvent(topic,message));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG,"deliveryComplete");
    }
}
