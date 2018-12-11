package com.example.zjf.mqtt.notification;

import java.io.Serializable;

public class Notification implements Serializable {
    int notify_type;
    String sendClientId;

    public int getNotify_type() {
        return notify_type;
    }
    public void setNotify_type(int notify_type) {
        this.notify_type = notify_type;
    }

    public String getSendClientId() {
        return sendClientId;
    }
    public void setSendClientId(String sendClientId) {
        this.sendClientId = sendClientId;
    }
}
