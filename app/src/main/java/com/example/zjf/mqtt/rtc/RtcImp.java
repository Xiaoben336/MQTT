package com.example.zjf.mqtt.rtc;

import android.app.Activity;

import com.example.zjf.mqtt.App;
import com.example.zjf.mqtt.notification.common.IceServer;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;

import java.util.LinkedList;
import java.util.List;


public class RtcImp implements RtcItf {
    @Override
    public void initPeerConnection() {
        App.getInst().setPeerConnectionClient(RTCConnection.getInstance());
    }

    @Override
    public void createPeerConnectionFactory(Activity owner, Object localRender,Object eglBaseContext,PeerConnectionParameters peerConnectionParameters) {
        App.getInst().getPeerConnectionClient().createPeerConnectionFactory(owner,(VideoRenderer.Callbacks)localRender,(EglBase.Context)eglBaseContext,peerConnectionParameters);
    }


    @Override
    public void createPeerConnection(String uid,Object remoteRender, List<IceServer> iceServers, Object type,boolean isCallOut,final PeerConnectionEvents events) {
        List<PeerConnection.IceServer> p_iceServers = new LinkedList<PeerConnection.IceServer>();
        for(IceServer server:iceServers){
            String username="";
            String password="";
            if(server.getUsername()!=null){
                username = server.getUsername();
            }
            if(server.getPassword()!=null){
                password = server.getPassword();
            }
            PeerConnection.IceServer p_server = new PeerConnection.IceServer(server.getUri(), username,password);
            p_iceServers.add(p_server);
        }
        App.getInst().getPeerConnectionClient().createPeerConnection(uid,(VideoRenderer.Callbacks)remoteRender,p_iceServers,isCallOut,(PeerConnection.IceTransportsType)type,events);
    }

    @Override
    public void createOffer(String uid) {
        App.getInst().getPeerConnectionClient().createOffer(uid);
    }

    @Override
    public void setRemoteDescription(String uid,Object offerSdp) {
        App.getInst().getPeerConnectionClient().setRemoteDescription(uid,(SessionDescription)offerSdp);
    }

    @Override
    public void createAnswer(String uid) {
        App.getInst().getPeerConnectionClient().createAnswer(uid);
    }

    @Override
    public void addRemoteIceCandidate(String uid,Object iceCandidate) {
        App.getInst().getPeerConnectionClient().addRemoteIceCandidate(uid,(IceCandidate) iceCandidate);
    }


    @Override
    public void enableStatsEvents(boolean b, int statCallbackPeriod) {
        App.getInst().getPeerConnectionClient().enableStatsEvents(b,statCallbackPeriod);
    }

    @Override
    public void stopVideoSource() {
        App.getInst().getPeerConnectionClient().stopVideoSource();
    }

    @Override
    public void close() {
        App.getInst().getPeerConnectionClient().close();
        App.getInst().setPeerConnectionClient(null);
    }

    @Override
    public void switchCamera() {
        App.getInst().getPeerConnectionClient().switchCamera();
    }

    @Override
    public void close(String uid) {
        App.getInst().getPeerConnectionClient().close(uid);
    }

    @Override
    public void getBaseState() {
        App.getInst().getPeerConnectionClient().getBaseStats();
    }

    @Override
    public void setVideoEnabled(boolean flag){
        App.getInst().getPeerConnectionClient().setVideoEnabled(flag);
    }
}
