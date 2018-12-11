package com.example.zjf.mqtt.notification;

import com.example.zjf.mqtt.notification.Notification;
import com.example.zjf.mqtt.notification.common.IceServer;

import java.util.LinkedList;

public class WebRtcNotification extends Notification {
    String msg;
    String topic;
    LinkedList<IceServer> iceServers;

    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public LinkedList<IceServer> getIceServers() {
        return iceServers;
    }
    public void setIceServers(LinkedList<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

}
