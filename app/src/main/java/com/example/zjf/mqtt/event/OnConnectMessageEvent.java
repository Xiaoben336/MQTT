package com.example.zjf.mqtt.event;

public class OnConnectMessageEvent {
    private String topic;
    public OnConnectMessageEvent(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
