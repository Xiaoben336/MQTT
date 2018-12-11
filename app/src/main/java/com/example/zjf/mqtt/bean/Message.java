package com.example.zjf.mqtt.bean;

public class Message {
    public String string;
    public boolean isLeft;

    public Message(String string,boolean isLeft){
        this.string = string;
        this.isLeft = isLeft;
    }
}
