package com.example.zjf.mqtt.notification;

public class CallNotification extends Notification {
    String msg;

    String topic;

    public String getTopic() {
        return topic;
    }

    public String getMsg() {
        return msg;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
