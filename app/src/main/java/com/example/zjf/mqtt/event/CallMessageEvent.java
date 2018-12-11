package com.example.zjf.mqtt.event;

public class CallMessageEvent {
    private String message;
    public  CallMessageEvent(String message){
        this.message=message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
