package com.example.zjf.mqtt.listener;

public interface VideoCallListener {
    void onInvite(String var1, String var2);

    void onAccept(String var1);

    void onBye(String var1);

    void onBusy(String var1);

    void onConnect(String var1);

    void onChannelClose(Long var1);

}
