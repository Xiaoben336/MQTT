package com.example.zjf.mqtt.bean;

import com.example.zjf.mqtt.notification.common.IceServer;

import java.util.LinkedList;

public class IceServerInfo implements Cloneable {
    LinkedList<IceServer> iceServers;

    public LinkedList<IceServer> getIceServers() {
        return iceServers;
    }

    public void setIceServers(LinkedList<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    @Override
    public IceServerInfo clone() {
        IceServerInfo user = null;
        try {
            user = (IceServerInfo) super.clone();
            if(iceServers!=null) {
                LinkedList<IceServer> newice = new LinkedList<IceServer>();
                for (IceServer server : iceServers) {
                    IceServer clone = new IceServer(server.uri, server.username, server.password);
                    newice.add(clone);
                }
                user.setIceServers(newice);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return user;
    }
}
