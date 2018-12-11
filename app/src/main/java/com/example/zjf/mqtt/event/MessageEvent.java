package com.example.zjf.mqtt.event;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MessageEvent {
    private String strTopic;
    private MqttMessage mqttMessage;
    private String str = "";

    public MessageEvent(String strTopic,MqttMessage mqttMessage){
        this.strTopic = strTopic;
        this.mqttMessage = mqttMessage;
    }

    public MessageEvent(String str){this.str = str;}

    public String getStr() {
        return str;
    }


    public MqttMessage getMqttMessage() {
        return mqttMessage;
    }

    public String getStrTopic() {
        return strTopic;
    }

    public void setStr(String str){
        this.str = str;
    }

    public void setMqttMessage(MqttMessage mqttMessage) {
        this.mqttMessage = mqttMessage;
    }

    public void setStrTopic(String strTopic) {
        this.strTopic = strTopic;
    }
}
