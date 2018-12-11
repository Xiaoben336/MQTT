package com.example.zjf.mqtt.event;

public class OnInviteMessageEvent extends CallMessageEvent{
    public OnInviteMessageEvent(String message) {
        super(message);
    }

    /*private String message;
    public  OnInviteMessageEvent(){
        this.message=message;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }*/
}