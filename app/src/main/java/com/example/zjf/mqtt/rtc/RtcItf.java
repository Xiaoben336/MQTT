package com.example.zjf.mqtt.rtc;

import android.app.Activity;

import com.example.zjf.mqtt.notification.common.IceServer;

import java.util.List;

public interface RtcItf {
    void initPeerConnection();

    void createPeerConnectionFactory(Activity var1, Object var2, Object var3, PeerConnectionParameters var4);

    void createPeerConnection(String var1, Object var3, List<IceServer> var4, Object var5, boolean var6, PeerConnectionEvents var7);

    void createOffer(String var1);

    void setRemoteDescription(String var1, Object var3);

    void createAnswer(String var1);

    void addRemoteIceCandidate(String var1, Object var3);

    void enableStatsEvents(boolean var1, int var2);

    void stopVideoSource();

    void close();

    void switchCamera();

    void close(String var1);

    void getBaseState();

    void setVideoEnabled(boolean var1);
}
