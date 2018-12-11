package com.example.zjf.mqtt.rtc;

public interface PeerConnectionEvents {
    void onLocalDescription(Object var1);

    void onIceCandidate(Object var1);

    void onIceCandidatesRemoved(Object[] var1);

    void onIceConnected();

    void onIceDisconnected();

    void onPeerConnectionClosed();

    void onPeerConnectionStatsReady(Object[] var1);

    void onPeerConnectionError(String var1);
}
