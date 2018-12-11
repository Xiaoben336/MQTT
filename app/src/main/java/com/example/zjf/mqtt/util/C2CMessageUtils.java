package com.example.zjf.mqtt.util;

import android.util.Log;

import com.example.zjf.mqtt.MyApplication;
import com.example.zjf.mqtt.mqtt.MqttUtils;
import com.example.zjf.mqtt.notification.Notification;
import com.example.zjf.mqtt.notification.NotificationTypeDef;
import com.example.zjf.mqtt.notification.common.IceServer;

import org.webrtc.PeerConnection;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class C2CMessageUtils {
    private static final String TAG = "C2CMessageUtils";
    private static final Lock sendLock = new ReentrantLock();

    public static void sendMessageByTopic(String topic,Notification sendObj){
        Log.d(TAG,"topic === " + topic+"   clientId === sendObj.getMsg === " + JsonUtils.objectToJson(sendObj));
        sendLock.lock();

        try {
            if (topic == null || topic.length() <= 0) {
                Log.d(TAG,"主题为空");
                return;
            }

            if (sendObj.getNotify_type() == NotificationTypeDef.C2C_NOTIFY_TYPE_WEBRTC) {//发送的WEBRTC信息
                try {
                    Field field = sendObj.getClass().getDeclaredField("iceServers");
                    field.setAccessible(true);
                    LinkedList<IceServer> iceServers = (LinkedList<IceServer>) field.get(sendObj);
                    List<PeerConnection.IceServer> p_iceServers = new LinkedList<PeerConnection.IceServer>();
                    if (iceServers != null) {
                        for (IceServer s : iceServers) {
                            PeerConnection.IceServer p_server = new PeerConnection.IceServer(s.getUri(), s.getUsername(), s.getPassword());
                            p_iceServers.add(p_server);
                        }
                        field.set(sendObj, p_iceServers);
                    }

                } catch (NoSuchFieldException e) {
                    Log.d("main", "NoSuchFieldException");
                } catch (IllegalAccessException e) {
                    Log.d("main", "IllegalAccessException");
                }
            }
            MqttUtils.getInstance(MyApplication.application).publish(topic,JsonUtils.objectToJson(sendObj));
        } finally {
            sendLock.unlock();
        }
    }
}
