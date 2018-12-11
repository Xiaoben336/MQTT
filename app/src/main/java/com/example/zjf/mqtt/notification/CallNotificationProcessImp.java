package com.example.zjf.mqtt.notification;

import com.example.zjf.mqtt.util.JsonUtils;

public class CallNotificationProcessImp implements NotificationProcessItf {
    @Override
    public void process(String msg) {
        final CallNotification notification = (CallNotification) JsonUtils.jsonToBean(msg,CallNotification.class);
    }
}
